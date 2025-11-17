//package org.andreidodu.nocconvert.gui.worker.hybrid.util;
//
//import org.andreidodu.nocconvert.exception.ManualAbortedException;
//import org.andreidodu.nocconvert.gui.worker.hybrid.listener.FilesInDirectoryListenerImpl;
//import org.andreidodu.nocconvert.task.PictureSearcherTask;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import java.io.File;
//import java.nio.file.AccessDeniedException;
//import java.nio.file.Path;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
//
//public class SearchUtil {
//    private static final Logger log = LogManager.getLogger(SearchUtil.class);
//
//    public long countFilesInDirectory(Path sourceDirectory) {
//        log.debug("Starting to search image from directory {}", sourceDirectory);
//        FilesInDirectoryListenerImpl filesInDirectoryListener = new FilesInDirectoryListenerImpl();
//        try {
//            List<Path> validSearchResult = new PictureSearcherTask().search(sourceDirectory, filesInDirectoryListener).stream().map(File::toPath).toList();
//            log.debug("Finished searching image from directory {}", sourceDirectory);
//            if (filesInDirectoryListener.getError() != null) {
//                throw filesInDirectoryListener.getError();
//            }
//            // validationPictureTask = new ValidationPictureTask(rawSearchResult, filesInDirectoryListener, super::isCancelled);
//            // validSearchResult = validationPictureTask.validateAndGetSearchResult();
//            return validSearchResult.size();
//        } catch (AccessDeniedException e) {
//            filesInDirectoryListener.onAccessDenied();
//            log.error("Access denied for: {}", sourceDirectory, e);
//        } catch (Exception e) {
//            if (e instanceof ManualAbortedException) {
//                filesInDirectoryListener.onOperationAborted();
//                filesInDirectoryListener.onOperationAborted();
//                return 9;
//            }
//            log.error(getRootCauseMessage(e), e);
//            onSearchComplete.accept(new ArrayList<>());
//            filesInDirectoryListener.onUpdateTotalFile(0L);
//        }
//        return 0;
//    }
//
//}
