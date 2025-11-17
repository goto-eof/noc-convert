package org.andreidodu.nocconvert.gui.worker.hybrid;

import lombok.Getter;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.IntWrapper;
import org.andreidodu.nocconvert.dto.conversion.input.ConversionWorkerInputDTO;
import org.andreidodu.nocconvert.dto.conversion.input.SearchAndConvertInputDTO;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.helper.ImageConverterUtil;
import org.andreidodu.nocconvert.helper.performance.AdaptiveSimpleGovernorRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.andreidodu.nocconvert.helper.performance.AdaptiveSimpleGovernorRunnable.calculateSafeValueWithoutXPercent;
import static org.andreidodu.nocconvert.mapper.ConcurrentItemDTOMapper.convertPathListToDTOList;
import static org.apache.commons.io.file.PathUtils.getExtension;

public class SearchAndConvertWorker extends SwingWorker<Void, Integer> {
    private static final Logger log = LogManager.getLogger(SearchAndConvertWorker.class);
    public static final int BUCKET_SIZE = 10_000;
    private final SearchAndConvertInputDTO searchAndConvertInputDTO;
    @Getter
    private final ExecutorService virtualThreadExecutor;
    @Getter
    private final ExecutorService platformExecutorService;
    @Getter
    private final int platformThreadsPermits = AdaptiveSimpleGovernorRunnable.IDEAL_NUM_OF_THREADS.get();

    @Getter
    private final int virtualThreadsPermits = calculateSafeValueWithoutXPercent(platformThreadsPermits, 50);

    private long lastTime = System.currentTimeMillis();

    private final IntWrapper indexIntWrapper = new IntWrapper(0);


    public SearchAndConvertWorker(SearchAndConvertInputDTO searchAndConvertInputDTO) {
        this.searchAndConvertInputDTO = searchAndConvertInputDTO;
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        platformExecutorService = Executors.newFixedThreadPool(platformThreadsPermits);
    }

    @Override
    protected Void doInBackground() throws Exception {
        searchAndConvertInputDTO.showJListPane().accept(true);
        List<String> availableExtensionList =
                new ImageConverterUtil().getAvailableReadFormatList()
                        .stream()
                        .map(FormatExtensionDTO::getExtension)
                        .map(String::toLowerCase)
                        .toList();

        try (Stream<Path> walkStream = Files.walk(searchAndConvertInputDTO.sourceDirectory())) {
            Semaphore walkSemaphore = new Semaphore(1);
            CountDownLatch countDownLatchWorkFinished = new CountDownLatch(1);
            List<Path> bucketList = new ArrayList<>();

            Iterator<Path> walkIterator = walkStream.iterator();
            searchAndConvertInputDTO.updateSecondaryProgressBarMaxValue().accept(BUCKET_SIZE);

            while (walkIterator.hasNext()) {

                Path path = walkIterator.next();
                if (!Files.isRegularFile(path) || !availableExtensionList.contains(getExtension(path.getFileName()).toLowerCase())) {
                    continue;
                }

                bucketList.add(path);
                long now = System.currentTimeMillis();
                if (now - lastTime >= 1000) {
                    searchAndConvertInputDTO.onFileFound().accept(bucketList.size(), path);
                    lastTime = now;
                }

                if (bucketList.size() >= BUCKET_SIZE) {
                    walkSemaphore.acquire();
                    searchAndConvertInputDTO.updateSecondaryProgressBarMaxValue().accept(bucketList.size());

                    List<ConversionItemDTO> conversionItemDTOList = convertPathListToDTOList(searchAndConvertInputDTO.destinationDirectory(), searchAndConvertInputDTO.targetFormat(), bucketList, indexIntWrapper);
                    ConversionWorkerInputDTO conversionWorkerInputDTO = buildInput(
                            conversionItemDTOList,
                            countDownLatchWorkFinished,
                            walkSemaphore,
                            virtualThreadExecutor,
                            virtualThreadsPermits,
                            platformExecutorService
                    );
                    awaitConvertion(conversionWorkerInputDTO, bucketList);
                }
            }

            if (!bucketList.isEmpty()) {
                walkSemaphore.acquire();
                searchAndConvertInputDTO.updateSecondaryProgressBarMaxValue().accept(bucketList.size());
                List<ConversionItemDTO> conversionItemDTOList = convertPathListToDTOList(searchAndConvertInputDTO.destinationDirectory(), searchAndConvertInputDTO.targetFormat(), bucketList, indexIntWrapper);
                ConversionWorkerInputDTO conversionWorkerInputDTO = buildInput(
                        conversionItemDTOList,
                        countDownLatchWorkFinished,
                        walkSemaphore,
                        virtualThreadExecutor,
                        virtualThreadsPermits,
                        platformExecutorService
                );
                awaitConvertion(conversionWorkerInputDTO, bucketList);
            }
        } catch (Exception e) {
            log.error("error: {}", e.getMessage(), e);
        } finally {
            try {
                waitForCompletionAndShutdownGentlyVirtualThreadExecutor();
            } catch (InterruptedException e) {
                log.debug("Interrupted while waiting for virtualThreadExecutor to terminate.");
            }

            try {
                waitForCompletionAndShutdownGentlyPlatformThreadExecutor();
            } catch (InterruptedException e) {
                log.debug("Interrupted while waiting for platformExecutorService to terminate.");
            }
        }

        return null;
    }

    private void awaitConvertion(ConversionWorkerInputDTO conversionWorkerInputDTO, List<Path> bucketList) throws InterruptedException {
        searchAndConvertInputDTO.endSearchChunkForImagesStep().accept(conversionWorkerInputDTO.list());
        ConvertWorker convertWorker = new ConvertWorker(conversionWorkerInputDTO);
        convertWorker.execute();
        conversionWorkerInputDTO.countDownLatchWorkFinished().await();

        convertWorker = null;
        publish(conversionWorkerInputDTO.list().size());
        searchAndConvertInputDTO.updateSecondaryProgressBarColorColorToBlue().run();
        bucketList.clear();
        searchAndConvertInputDTO.resetSecondaryProgressBar().run();

    }

    private ConversionWorkerInputDTO buildInput(
            List<ConversionItemDTO> pathList,
            CountDownLatch countDownLatchWorkFinished,
            Semaphore semaphore,
            ExecutorService virtualThreadsExecutor,
            int virtualThreadsPermits,
            ExecutorService platformExecutorService
    ) {
        return ConversionWorkerInputDTO.builder()
                .list(pathList.stream().toList())
                .updateGui(searchAndConvertInputDTO.updateGui())
                .onAllTasksComplete(searchAndConvertInputDTO.onBucketItemsCompleted())
                .completeItem(searchAndConvertInputDTO.completeItem())
                .onConversionAborted(searchAndConvertInputDTO.onConversionAborted())
                .incrementPasses(searchAndConvertInputDTO.incrementPasses())
                .incrementFailures(searchAndConvertInputDTO.incrementFailures())
                .decrementPasses(searchAndConvertInputDTO.decrementPasses())
                .addToDeletionQueue(searchAndConvertInputDTO.addToDeletionQueue())
                .countDownLatchWorkFinished(countDownLatchWorkFinished)
                .semaphore(semaphore)
                .virtualThreadsExecutor(virtualThreadsExecutor)
                .virtualThreadsPermits(virtualThreadsPermits)
                .platformThreadsExecutor(platformExecutorService)
                .build();
    }

    @Override
    protected void process(List<Integer> items) {
        searchAndConvertInputDTO.incrementMainProgressBarProgress()
                .accept(items.stream().mapToInt(Integer::intValue).sum());
    }

    @Override
    protected void done() {
        searchAndConvertInputDTO.onAllTasksComplete().run();
    }

    public Void manualShutdownThreads() {
        return null;
    }

    private void waitForCompletionAndShutdownGentlyVirtualThreadExecutor() throws InterruptedException {
        getVirtualThreadExecutor().shutdown();
        boolean terminated = getVirtualThreadExecutor().awaitTermination(31, TimeUnit.DAYS);
        if (!terminated) {
            log.error("virtualThreadExecutor did not terminate in time. Forcing emergency shutdown.");
            getVirtualThreadExecutor().shutdownNow();
        }
    }

    private void waitForCompletionAndShutdownGentlyPlatformThreadExecutor() throws InterruptedException {
        getPlatformExecutorService().shutdown();
        boolean platformExecutorShutdown = getPlatformExecutorService().awaitTermination(31, TimeUnit.DAYS);
        if (!platformExecutorShutdown) {
            log.error("platformExecutorService did not terminate in time. Forcing emergencyConversionWorker shutdown.");
            getPlatformExecutorService().shutdownNow();
        }
    }

}
