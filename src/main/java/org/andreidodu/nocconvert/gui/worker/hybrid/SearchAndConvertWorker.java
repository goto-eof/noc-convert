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
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.andreidodu.nocconvert.constants.ApplicationConfig.FINAL_OUTPUT_DIRECTORY_NAME;
import static org.andreidodu.nocconvert.constants.ApplicationConfig.TMP_OUTPUT_DIRECTORY_NAME;
import static org.andreidodu.nocconvert.helper.performance.AdaptiveSimpleGovernorRunnable.calculateSafeValueWithoutXPercent;
import static org.andreidodu.nocconvert.mapper.ConcurrentItemDTOMapper.convertPathListToDTOList;
import static org.apache.commons.io.file.PathUtils.getExtension;

public class SearchAndConvertWorker extends SwingWorker<Void, Integer> {
    private static final Logger log = LogManager.getLogger(SearchAndConvertWorker.class);
    public static final int NUMBER_OF_FILES_PER_DIRECTORY = 10_000;
    public static final int MINIMUM_BUCKET_SIZE = 10;
    public static AtomicInteger adaptiveBucketSize = new AtomicInteger(MINIMUM_BUCKET_SIZE);
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
    private final HardwareAbstractionLayer hal;
    private long fileCounter = 0;
    private Path temporaryDirectory;
    private Path destinationDirectory;

    public int getAdaptiveBucketSize() {
        return adaptiveBucketSize.get();
    }

    public void setAdaptiveBucketSize(int adaptiveBucketSize) {
        SearchAndConvertWorker.adaptiveBucketSize.set(adaptiveBucketSize);
    }

    public SearchAndConvertWorker(SearchAndConvertInputDTO searchAndConvertInputDTO) {
        this.searchAndConvertInputDTO = searchAndConvertInputDTO;
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        platformExecutorService = Executors.newFixedThreadPool(platformThreadsPermits);
        SystemInfo si = new SystemInfo();
        hal = si.getHardware();
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
        destinationDirectory = searchAndConvertInputDTO.destinationDirectory();
        temporaryDirectory = buildTmpPathAndReturn(destinationDirectory);

        try (Stream<Path> walkStream = Files.walk(searchAndConvertInputDTO.sourceDirectory())) {
            Semaphore walkSemaphore = new Semaphore(1);
            CountDownLatch countDownLatchWorkFinished = new CountDownLatch(1);
            List<Path> bucketList = new ArrayList<>();

            Iterator<Path> walkIterator = walkStream.iterator();


            while (walkIterator.hasNext()) {

                Path path = walkIterator.next();
                if (!Files.isRegularFile(path) || !availableExtensionList.contains(getExtension(path.getFileName()).toLowerCase())) {
                    continue;
                }

                bucketList.add(path);
                fileCounter++;


                long now = System.currentTimeMillis();
                if (now - lastTime >= 1000) {
                    searchAndConvertInputDTO.onFileFound().accept(bucketList.size(), path);
                    lastTime = now;
                }

                int adaptiveBucketSize = recalculateAdaptiveBucketSize();
                if (bucketList.size() >= adaptiveBucketSize) {
                    // TODO: insert a sleep with a variable duration somewhere here
                    // NOTE: because the supplier (noc-convert) produces so many files that the consumer (gvfsd-metadata) is not able to process without increasing the RAM usage (minimum 15Gb for 5 million files).
                    // NOTE: we need to monitor indirectly the activity of nautilus and especially of gvfsd-metadata
                    // NOTE; the calculated duration shall be chose dynamically in base of the current RAM consumption level (we could have also other processes that eat RAM at some point)
                    Path tmpDir = temporaryDirectory;
                    walkSemaphore.acquire();
                    countDownLatchWorkFinished = new CountDownLatch(1);
                    searchAndConvertInputDTO.updateSecondaryProgressBarMaxValue().accept(bucketList.size());

                    List<ConversionItemDTO> conversionItemDTOList = convertPathListToDTOList(searchAndConvertInputDTO.destinationDirectory(), searchAndConvertInputDTO.targetFormat(), bucketList, indexIntWrapper);
                    ConversionWorkerInputDTO conversionWorkerInputDTO = buildInput(
                            conversionItemDTOList,
                            countDownLatchWorkFinished,
                            walkSemaphore,
                            virtualThreadExecutor,
                            virtualThreadsPermits,
                            platformExecutorService,
                            tmpDir,
                            destinationDirectory
                    );
                    searchAndConvertInputDTO.updateSecondaryProgressBarMaxValue().accept(adaptiveBucketSize);
                    awaitConvertion(conversionWorkerInputDTO);
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
                        platformExecutorService,
                        temporaryDirectory,
                        destinationDirectory
                );
                awaitConvertion(conversionWorkerInputDTO);
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

    private int recalculateAdaptiveBucketSize() {
        int adaptiveBucketSize = this.getAdaptiveBucketSize();
        GlobalMemory memory = hal.getMemory();
        long total = memory.getTotal();
        long onePerc = total / 100;
        long available = memory.getAvailable();
        int currentPerc = (int) ((total - available) / onePerc);
        if (currentPerc < 50 && adaptiveBucketSize < NUMBER_OF_FILES_PER_DIRECTORY) {
            adaptiveBucketSize = adaptiveBucketSize * MINIMUM_BUCKET_SIZE;
            log.warn("\n\nbucketSize increased to {}\n\n", adaptiveBucketSize);
        } else if (currentPerc > 50 && adaptiveBucketSize > MINIMUM_BUCKET_SIZE) {
            int newValue = adaptiveBucketSize / MINIMUM_BUCKET_SIZE;
            adaptiveBucketSize = Math.max(newValue, MINIMUM_BUCKET_SIZE);
            log.warn("\n\nbucketSize decreased" +
                    " to {}\n\n", adaptiveBucketSize);
        }
        setAdaptiveBucketSize(adaptiveBucketSize);
        return adaptiveBucketSize;
    }

    private Path calculateFinalDir(Path destinationDirectory) {
        return Path.of(destinationDirectory.toString(), FINAL_OUTPUT_DIRECTORY_NAME);
    }

//    private Path calculateTemporaryDir(Path destinationDirectory) {
//        return Path.of(destinationDirectory.toString(), FINAL_OUTPUT_DIRECTORY_NAME);
//    }

    private static Path buildTmpPathAndReturn(Path destinationDirectory) throws IOException {
        Path tmpDirPath = Path.of(destinationDirectory.toString(), TMP_OUTPUT_DIRECTORY_NAME + "-" + UUID.randomUUID());
        Files.createDirectories(tmpDirPath);
        return tmpDirPath;
    }

    private void awaitConvertion(ConversionWorkerInputDTO conversionWorkerInputDTO) throws InterruptedException {
        searchAndConvertInputDTO.endSearchChunkForImagesStep().accept(conversionWorkerInputDTO.list());
        ConvertWorker convertWorker = new ConvertWorker(conversionWorkerInputDTO);
        convertWorker.execute();

        conversionWorkerInputDTO.countDownLatchWorkFinished().await();

        convertWorker = null;
        publish(conversionWorkerInputDTO.list().size());
        searchAndConvertInputDTO.updateSecondaryProgressBarColorColorToBlue().run();
        searchAndConvertInputDTO.resetSecondaryProgressBar().run();

        if (fileCounter >= NUMBER_OF_FILES_PER_DIRECTORY) {
            try {
                temporaryDirectory = buildTmpPathAndReturn(destinationDirectory);
                fileCounter = 0;
            } catch (IOException e) {
                throw new RuntimeException("Oooooooops!");
            }
        } else {

        }
    }

    private ConversionWorkerInputDTO buildInput(
            List<ConversionItemDTO> conversionItemDTOList,
            CountDownLatch countDownLatchWorkFinished,
            Semaphore semaphore,
            ExecutorService virtualThreadsExecutor,
            int virtualThreadsPermits,
            ExecutorService platformExecutorService,
            Path temporaryDirectory,
            Path destinationDirectory
    ) {
        return ConversionWorkerInputDTO.builder()
                .list(conversionItemDTOList.stream().toList())
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
                .finalDirectory(calculateFinalDir(destinationDirectory))
                .temporaryDirectory(temporaryDirectory)
                .updateTmpDestinationDirectory(this::updateTmpDestinationDirectory)
                .build();
    }

    private void updateTmpDestinationDirectory(Path path) {
        this.temporaryDirectory = path;
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
