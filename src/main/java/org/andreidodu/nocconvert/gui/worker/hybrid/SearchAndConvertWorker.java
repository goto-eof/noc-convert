package org.andreidodu.nocconvert.gui.worker.hybrid;

import lombok.Getter;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.IntWrapper;
import org.andreidodu.nocconvert.dto.conversion.input.ConversionWorkerInputDTO;
import org.andreidodu.nocconvert.dto.conversion.input.SearchAndConvertInputDTO;
import org.andreidodu.nocconvert.enums.DEFCON;
import org.andreidodu.nocconvert.exception.ConversionManualAbortedException;
import org.andreidodu.nocconvert.exception.ManualAbortedException;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.gui.worker.hybrid.defcon.DefconLevel1Task;
import org.andreidodu.nocconvert.gui.worker.hybrid.defcon.DefconLevel2Task;
import org.andreidodu.nocconvert.gui.worker.hybrid.defcon.DefconLevel3Task;
import org.andreidodu.nocconvert.util.ImageConverterUtil;
import org.andreidodu.nocconvert.util.RamUtil;
import org.andreidodu.nocconvert.util.performance.AdaptiveSimpleGovernorRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import oshi.SystemInfo;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.andreidodu.nocconvert.constants.ApplicationConfig.*;
import static org.andreidodu.nocconvert.mapper.ConcurrentItemDTOMapper.convertPathListToDTOList;
import static org.andreidodu.nocconvert.util.performance.AdaptiveSimpleGovernorRunnable.calculateSafeValueWithoutXPercent;
import static org.apache.commons.io.file.PathUtils.getExtension;

public class SearchAndConvertWorker extends SwingWorker<Void, Integer> {
    private static final Logger log = LogManager.getLogger(SearchAndConvertWorker.class);


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
    private final AtomicReference<DEFCON> DEFCONLevel = new AtomicReference<>(DEFCON.LEVEL_3);
    private final AtomicInteger defcon3SecondsToWait = new AtomicInteger(0);
    private final RamUtil ramUtil;
    @Getter
    private final AtomicBoolean manualShutdown = new AtomicBoolean(false);
    private ConvertWorker convertWorker;
    private final ConcurrentLinkedQueue<Path> toDeletePathList = new ConcurrentLinkedQueue<>();

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
        this.ramUtil = new RamUtil();
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
            int ramUsagePercentage = ramUtil.getCurrentOccupiedRAMPercentage();
            int adaptiveBucketSize = recalculateAdaptiveBucketSize(ramUsagePercentage);

            while (walkIterator.hasNext()) {

                checkAndAbortIfNecessary();

                applyDefcon2IfNecessary();

                checkAndAbortIfNecessary();

                applyDefcon1IfNecessary();

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

                checkAndAbortIfNecessary();

                if (bucketList.size() >= adaptiveBucketSize) {
                    ramUsagePercentage = ramUtil.getCurrentOccupiedRAMPercentage();
                    adaptiveBucketSize = recalculateAdaptiveBucketSize(ramUsagePercentage);

                    if (DEFCON.LEVEL_3.equals(DEFCONLevel.get())) {
                        increaseDefcon3SecondsToWaitIfNecessary(ramUsagePercentage, adaptiveBucketSize);
                        if (DEFCON.LEVEL_3.equals(DEFCONLevel.get())) {
                            applyDEFCONLevel3();
                        } else {
                            applyDefcon2IfNecessary();
                        }
                    }

                    checkAndAbortIfNecessary();


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
                        temporaryDirectory,
                        destinationDirectory
                );
                awaitConvertion(conversionWorkerInputDTO);
            }
        } catch (Exception e) {
            log.error("error: {}", e.getMessage(), e);
            if (e instanceof ManualAbortedException) {
                searchAndConvertInputDTO.onConversionAborted().run();
            }
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

    private void checkAndAbortIfNecessary() {
        if (manualShutdown.get()) {
            throw new ConversionManualAbortedException("manual shutdown");
        }
    }

    private void applyDefcon1IfNecessary() throws InterruptedException {
        if (DEFCON.LEVEL_1.equals(DEFCONLevel.get())) {
            applyDEFCONLevel1();
            int newRaUsagePercentage = ramUtil.getCurrentOccupiedRAMPercentage();

            if (newRaUsagePercentage < DEFCON.LEVEL_3.getCriticalPoint()) {
                DEFCONLevel.set(DEFCON.LEVEL_3);
            } else if (newRaUsagePercentage < DEFCON.LEVEL_2.getCriticalPoint()) {
                DEFCONLevel.set(DEFCON.LEVEL_3);
            } else if (newRaUsagePercentage < DEFCON.LEVEL_1.getCriticalPoint()) {
                DEFCONLevel.set(DEFCON.LEVEL_2);
            }
        }
    }

    private void applyDefcon2IfNecessary() throws InterruptedException {
        if (DEFCON.LEVEL_2.equals(DEFCONLevel.get())) {
            applyDEFCONLevel2();
            int newRaUsagePercentage = ramUtil.getCurrentOccupiedRAMPercentage();
            if (newRaUsagePercentage >= DEFCON.LEVEL_1.getCriticalPoint()) {
                DEFCONLevel.set(DEFCON.LEVEL_1);
            } else if (newRaUsagePercentage >= DEFCON.LEVEL_2.getCriticalPoint()) {
                DEFCONLevel.set(DEFCON.LEVEL_2);
            } else {
                DEFCONLevel.set(DEFCON.LEVEL_3);
            }
        }
    }

    private void applyDEFCONLevel1() throws InterruptedException {
        CountDownLatch defconLevel1Latch = new CountDownLatch(1);
        Thread.ofVirtual().name("defcon-level-1-strategy").start(new DefconLevel1Task(defconLevel1Latch, getManualShutdown()::get));
        defconLevel1Latch.await();
    }

    private void applyDEFCONLevel2() throws InterruptedException {
        CountDownLatch defconLevel2Latch = new CountDownLatch(1);
        Thread.ofVirtual().name("defcon-level-2-strategy").start(new DefconLevel2Task(defconLevel2Latch, getManualShutdown()::get));
        defconLevel2Latch.await();
    }

    private void applyDEFCONLevel3() throws InterruptedException {
        if (defcon3SecondsToWait.get() <= 0) {
            return;
        }
        CountDownLatch defconLevel3Latch = new CountDownLatch(1);
        Thread.ofVirtual().name("defcon-level-3-strategy").start(new DefconLevel3Task(defconLevel3Latch, defcon3SecondsToWait.get(), getManualShutdown()::get));
        defconLevel3Latch.await();
    }

    private int recalculateAdaptiveBucketSize(int currentRamUsagePercentage) {
        int adaptiveBucketSize = this.getAdaptiveBucketSize();
        if (currentRamUsagePercentage < DEFCON.LEVEL_3.getCriticalPoint() && adaptiveBucketSize < MAX_BUCKET_SIZE) {
            adaptiveBucketSize = adaptiveBucketSize * MINIMUM_BUCKET_SIZE;
            log.warn("\n\nbucketSize increased to {}\n\n", adaptiveBucketSize);
            if (defcon3SecondsToWait.get() - 5 >= 0) {
                defcon3SecondsToWait.set(defcon3SecondsToWait.get() - 5);
            }
        } else if (currentRamUsagePercentage > DEFCON.LEVEL_3.getCriticalPoint() && adaptiveBucketSize > MINIMUM_BUCKET_SIZE) {
            int newValue = adaptiveBucketSize / MINIMUM_BUCKET_SIZE;
            adaptiveBucketSize = Math.max(newValue, MINIMUM_BUCKET_SIZE);
            log.warn("\n\nbucketSize decreased to {}\n\n", adaptiveBucketSize);
        }

        setAdaptiveBucketSize(adaptiveBucketSize);
        return adaptiveBucketSize;
    }

    private void increaseDefcon3SecondsToWaitIfNecessary(int currentPerc, int adaptiveBucketSize) {
        if (currentPerc > DEFCON.LEVEL_3.getCriticalPoint()) {
            if (defcon3SecondsToWait.get() + 5 <= DEFCON_3_MAX_SECONDS_TO_WAIT) {
                defcon3SecondsToWait.set(defcon3SecondsToWait.get() + 5);
            }
            if (adaptiveBucketSize == MINIMUM_BUCKET_SIZE && defcon3SecondsToWait.get() >= DEFCON_3_MAX_SECONDS_TO_WAIT && currentPerc >= DEFCON.LEVEL_2.getCriticalPoint()) {
                defcon3SecondsToWait.set(DEFCON_3_MAX_SECONDS_TO_WAIT);
                DEFCONLevel.set(DEFCON.LEVEL_2);
            }
        }
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
        checkAndAbortIfNecessary();

        searchAndConvertInputDTO.endSearchChunkForImagesStep().accept(conversionWorkerInputDTO.list());
        convertWorker = new ConvertWorker(conversionWorkerInputDTO);
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
                .addToDeletionQueue(this::addToDeletionQueue)
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

    private void addToDeletionQueue(Path path) {
        this.toDeletePathList.add(path);
    }

    private void updateTmpDestinationDirectory(Path path) {
        this.temporaryDirectory = path;
        addToDeletionQueue(path);
    }

    @Override
    protected void process(List<Integer> items) {
        searchAndConvertInputDTO.incrementMainProgressBarProgress()
                .accept(items.stream().mapToInt(Integer::intValue).sum());
    }

    @Override
    protected void done() {
        searchAndConvertInputDTO.onAllTasksComplete().run();
        if (searchAndConvertInputDTO.jobDoneCountDownLatch() != null) {
            searchAndConvertInputDTO.jobDoneCountDownLatch().countDown();
        }
    }

    public Void manualShutdownThreads() {
        this.manualShutdown.set(true);
        searchAndConvertInputDTO.addAllToDeletionQueue().accept(this.toDeletePathList);
        if (this.convertWorker != null) {
            convertWorker.manualShutdownThreads();
            convertWorker = null;
        }
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

    public DEFCON getDefconLevel() {
        return this.DEFCONLevel.get();
    }

    public int getDefcon3SecondsToWait() {
        return defcon3SecondsToWait.get();
    }
}
