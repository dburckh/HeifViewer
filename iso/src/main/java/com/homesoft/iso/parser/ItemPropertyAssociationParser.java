package com.homesoft.iso.parser;

import androidx.annotation.NonNull;

import com.homesoft.iso.Box;
import com.homesoft.iso.BoxTypes;
import com.homesoft.iso.DataUtil;
import com.homesoft.iso.StreamReader;
import com.homesoft.iso.TypedBoxParser;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class ItemPropertyAssociationParser implements TypedBoxParser {
    @NonNull
    @Override
    public ItemPropertyAssociation[] parse(Box box, StreamReader streamReader, int versionFlags) throws IOException {
        final int entryCount = streamReader.getInt();
        final ItemPropertyAssociation[] associations = new ItemPropertyAssociation[entryCount];
        final int version = Box.getVersion(versionFlags);
        final int flags = Box.getFlags(versionFlags);
        boolean propertyIndexIsShort = (flags & 0x1) != 0;
        for (int i=0;i<entryCount;i++) {
            final int id;
            if (version < 1) {
                id = DataUtil.getUShort(streamReader);
            } else {
                id = streamReader.getInt();
            }
            int associationSize = DataUtil.getUByte(streamReader);
            if (propertyIndexIsShort) {
                associationSize *=2;
            }
            final byte[] buffer = streamReader.getBytes(associationSize);
            final ByteBuffer workBuffer = ByteBuffer.wrap(buffer);
            final Buffer associationBuffer = propertyIndexIsShort ? workBuffer.asShortBuffer() : workBuffer;

            associations[i] = new ItemPropertyAssociation(id, associationBuffer);
        }
        return associations;
    }

    @Override
    public boolean isFullBox() {
        return true;
    }

    @Override
    public int getType() {
        return BoxTypes.TYPE_ipma;
    }
}
