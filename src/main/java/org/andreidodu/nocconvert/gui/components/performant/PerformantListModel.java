package org.andreidodu.nocconvert.gui.components.performant;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PerformantListModel extends AbstractListModel<ConversionItemDTO> {

    private final List<ConversionItemDTO> data;

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
        int index = data.size();
        data.addAll(chunkList);
        fireIntervalAdded(this, index, data.size() - 1);
    }
}