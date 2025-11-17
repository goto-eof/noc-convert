package org.andreidodu.nocconvert.gui.components.performant;

import lombok.Getter;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PerformantListModel extends AbstractListModel<ConversionItemDTO> {

    public static final int MAX_NUM_ELEMENTS = 10;
    @Getter
    private final List<ConversionItemDTO> data;
    private JList<ConversionItemDTO> conversionFileList;

    public PerformantListModel() {
        this.data = Collections.synchronizedList(new ArrayList<>());
    }

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

    public void addAll(List<ConversionItemDTO> failedChunkList) {

        if (failedChunkList.isEmpty()) {
            return;
        }

        data.addAll(failedChunkList);

        if (failedChunkList.stream().mapToInt(ConversionItemDTO::getExternalIndex).min().orElse(data.size() + 1) > MAX_NUM_ELEMENTS) {
            return;
        }

        fireIntervalAdded(this, 0, MAX_NUM_ELEMENTS);
    }

    public void updateElements(List<ConversionItemDTO> failedChunkList) {
        if (failedChunkList.isEmpty()) {
            return;
        }

        int minIndex = Integer.MAX_VALUE;
        int maxIndex = Integer.MIN_VALUE;


        for (ConversionItemDTO item : failedChunkList) {
            if (data.isEmpty() || item.getExternalIndex() >= data.size()) {
                data.add(item);
            } else {
                data.set(item.getExternalIndex(), item);
            }

            if (conversionFileList != null) {
                minIndex = Math.min(minIndex, item.getExternalIndex());
                maxIndex = Math.max(maxIndex, item.getExternalIndex());
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