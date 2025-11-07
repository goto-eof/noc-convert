package org.andreidodu.nocconvert.gui.components.performant;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PerformantListModel extends AbstractListModel<ConversionItemDTO> {

    public static final int MAX_NUM_ELEMENTS = 10;
    private final List<ConversionItemDTO> data;
    private JList<ConversionItemDTO> conversionFileList;

    public PerformantListModel() {
        this.data = Collections.synchronizedList(new ArrayList<>());
    }

    public void updateElementAt(int index, ConversionItemDTO updatedItem) {
        if (index >= 0 && index < data.size()) {
            data.set(index, updatedItem);
            fireContentsChanged(this, index, index);
        }
    }

    public void addElement(ConversionItemDTO item) {
        int index = data.size();
        data.add(item);
        fireIntervalAdded(this, index, index);
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

    public void addAll(List<ConversionItemDTO> chunkList) {
        data.addAll(chunkList);
        fireIntervalAdded(this, 0, MAX_NUM_ELEMENTS);
    }

    public void updateElements(List<ConversionItemDTO> list) {
        int minIndex = Integer.MAX_VALUE;
        int maxIndex = Integer.MIN_VALUE;
        for (ConversionItemDTO item : list) {
            data.set(item.getIndex(), item);
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