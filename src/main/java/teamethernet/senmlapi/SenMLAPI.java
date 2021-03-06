/*
Copyright (c) 2019,
Anton Bothin,
Erik Flink,
Nelly Friman,
Jacob Klasmark,
Valter Lundegårdh,
Isak Olsson,
Andreas Sjödin,
Carina Wickström.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. The names of the authors may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHORS ``AS IS'' AND ANY EXPRESS
OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package teamethernet.senmlapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SenMLAPI<T extends Formatter> {

    private static final String STRING_INSTANCE = "";
    private static final Double DOUBLE_INSTANCE = 0.0;
    private static final Integer INTEGER_INSTANCE = 0;
    private static final Boolean BOOLEAN_INSTANCE = false;

    private final T formatter;

    private SenMLAPI(final T formatter) {
        this.formatter = formatter;
    }

    public static SenMLAPI<JsonFormatter> initJson() {
        return new SenMLAPI<>(new JsonFormatter());
    }

    public static SenMLAPI<JsonFormatter> initJson(final byte[] buffer) throws IOException {
        return new SenMLAPI<>(new JsonFormatter(buffer));
    }

    public static SenMLAPI<CborFormatter> initCbor() {
        return new SenMLAPI<>(new CborFormatter());
    }

    public static SenMLAPI<CborFormatter> initCbor(final byte[] buffer) throws IOException {
        return new SenMLAPI<>(new CborFormatter(buffer));
    }

    public List<byte[]> getRecords() throws IOException {
        final List<byte[]> records = new ArrayList<>();

        for (int i = 0; i < formatter.getRecords().size(); i++) {
            records.add(getRecord(i));
        }

        return records;
    }

    public byte[] getRecord(final int recordIndex) throws IOException {
        return formatter.getSenML(formatter.getRecords().get(recordIndex));
    }

    public List<Label> getLabels(final int recordIndex) {
        final JsonNode record = formatter.getRecords().get(recordIndex);
        final List<Label> labels = new ArrayList<>();

        record.fields().forEachRemaining(field ->
                labels.add(Label.getNameToValueMap(formatter.getClass()).get(field.getKey())));

        return labels;
    }

    @SuppressWarnings("unchecked")
    public <S> S getValue(Label<S> label, int recordIndex) {
        final Class<S> type = label.getClassType();
        final JsonNode record = formatter.getRecords().get(recordIndex);

        if (type.isInstance(STRING_INSTANCE)) {
            return type.cast(formatter.getStringValue((Label<String>) label, record));
        } else if (type.isInstance(DOUBLE_INSTANCE)) {
            return type.cast(formatter.getDoubleValue((Label<Double>) label, record));
        } else if (type.isInstance(INTEGER_INSTANCE)) {
            return type.cast(formatter.getIntegerValue((Label<Integer>) label, record));
        } else if (type.isInstance(BOOLEAN_INSTANCE)) {
            return type.cast(formatter.getBooleanValue((Label<Boolean>) label, record));
        } else {
            throw new UnsupportedOperationException(
                    type + " is not supported. Use String, Double, Integer or Boolean");
        }
    }

    public void addRecord(final byte[] record) throws IOException {
        formatter.addRecord(record);
    }

    public final void addRecord(final Label.Pair ... pairs) {
        final JsonNode record = formatter.getMapper().createObjectNode();

        for (final Label.Pair pair : pairs) {
            final Class<?> type = pair.getLabel().getClassType();

            if (type.isInstance(STRING_INSTANCE)) {
                ((ObjectNode) record).put(pair.getLabel().getFormattedLabel(formatter.getClass()), (String) pair.getValue());
            } else if (type.isInstance(DOUBLE_INSTANCE)) {
                ((ObjectNode) record).put(pair.getLabel().getFormattedLabel(formatter.getClass()), (Double) pair.getValue());
            } else if (type.isInstance(INTEGER_INSTANCE)) {
                ((ObjectNode) record).put(pair.getLabel().getFormattedLabel(formatter.getClass()), (Integer) pair.getValue());
            } else if (type.isInstance(BOOLEAN_INSTANCE)) {
                ((ObjectNode) record).put(pair.getLabel().getFormattedLabel(formatter.getClass()), (Boolean) pair.getValue());
            } else {
                throw new UnsupportedOperationException(
                        type + " is not supported. Use String, Double, Integer or Boolean");
            }
        }

        ((ArrayNode) formatter.getRecords()).add(record);
    }

    public byte[] getSenML() throws IOException {
        return formatter.getSenML(formatter.getRecords());
    }

}
