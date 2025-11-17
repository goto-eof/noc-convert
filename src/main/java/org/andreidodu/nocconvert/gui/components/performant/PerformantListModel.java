package org.andreidodu.nocconvert.gui.components.performant;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;

import javax.swing.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerformantListModel extends AbstractListModel<ConversionItemDTO> {

    public static final int MAX_NUM_ELEMENTS = 10;
    private final Map<Integer, ConversionItemDTO> data;
    private JList<ConversionItemDTO> conversionFileList;

    public PerformantListModel() {
        this.data = Collections.synchronizedMap(new HashMap<Integer, ConversionItemDTO>());
    }

//    public void updateElementAt(int index, ConversionItemDTO updatedItem) {
//        if (index >= 0 && index < data.size()) {
//            data.put(index, updatedItem);
//            fireContentsChanged(this, index, index);
//        }
//    }
//
//    public void addElement(ConversionItemDTO item) {
//        int index = data.size();
//        data.add(item);
//        fireIntervalAdded(this, index, index);
//    }

    @Override
    public int getSize() {
        return data.size();
    }

    @Override
    public ConversionItemDTO getElementAt(int index) {
        if (index >= 0 && index < data.size()) {
            return data.get(index);
        }
        return null;
    }

    public void addAll(List<ConversionItemDTO> chunkList) {
//        List<ConversionItemDTO> filtered = chunkList.stream().filter(item -> !ConversionStatus.COMPLETED.equals(item.getStatus())).toList();
//        data.addAll(filtered);
//
//        if (filtered.stream().mapToInt(ConversionItemDTO::getIndex).min().orElse(data.size() + 1) > MAX_NUM_ELEMENTS) {
//            return;
//        }
//
//        fireIntervalAdded(this, 0, MAX_NUM_ELEMENTS);
    }

    public void updateElements(List<ConversionItemDTO> list) {
        int minIndex = Integer.MAX_VALUE;
        int maxIndex = Integer.MIN_VALUE;

        List<ConversionItemDTO> filtered = list.stream()
                .filter(item -> ConversionStatus.FAILED.equals(item.getStatus()) || ConversionStatus.QUEUED.equals(item.getStatus()))
                .toList();

        for (ConversionItemDTO item : filtered) {
            if (item.getIndex() >= data.size()) {
                continue;
            }
            data.put(item.getIndex(), item);
            if (conversionFileList != null) {
                minIndex = Math.min(minIndex, item.getIndex());
                maxIndex = Math.max(maxIndex, item.getIndex());
            }
        }

        if (conversionFileList != null) {
            if (maxIndex < conversionFileList.getFirstVisibleIndex() || minIndex > conversionFileList.getLastVisibleIndex()) {
                return;
            }
            fireContentsChanged(this, 0, MAX_NUM_ELEMENTS);
        }
    }

    public void setJList(JList<ConversionItemDTO> conversionFileList) {
        this.conversionFileList = conversionFileList;
    }
}