/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.thing.xml.internal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.xml.util.ConverterAttributeMapValidator;
import org.openhab.core.config.xml.util.GenericUnmarshaller;
import org.openhab.core.config.xml.util.NodeIterator;
import org.openhab.core.config.xml.util.NodeList;
import org.openhab.core.config.xml.util.NodeValue;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link StateDescriptionConverter} is a concrete implementation of the {@code XStream} {@link Converter} interface
 * used to convert a state description within an XML document
 * into a {@link StateDescription} object.
 * <p>
 * This converter converts {@code state} XML tags.
 *
 * @author Michael Grammling - Initial contribution
 */
@NonNullByDefault
public class StateDescriptionConverter extends GenericUnmarshaller<StateDescription> {

    protected ConverterAttributeMapValidator attributeMapValidator;

    public StateDescriptionConverter() {
        super(StateDescription.class);

        this.attributeMapValidator = new ConverterAttributeMapValidator(new String[][] { { "min", "false" },
                { "max", "false" }, { "step", "false" }, { "pattern", "false" }, { "readOnly", "false" } });
    }

    private @Nullable BigDecimal toBigDecimal(Map<String, String> attributes, String attribute,
            @Nullable BigDecimal defaultValue) throws ConversionException {
        String attrValueText = attributes.get(attribute);

        if (attrValueText != null) {
            try {
                return new BigDecimal(attrValueText);
            } catch (NumberFormatException nfe) {
                throw new ConversionException(
                        "The attribute '" + attribute + "' has not a valid decimal number format!", nfe);
            }
        }

        return defaultValue;
    }

    private boolean toBoolean(Map<String, String> attributes, String attribute, boolean defaultValue) {
        String attrValueText = attributes.get(attribute);
        return attrValueText == null ? defaultValue : Boolean.valueOf(attrValueText);
    }

    private List<StateOption> toListOfChannelState(NodeList nodeList) throws ConversionException {
        if ("options".equals(nodeList.getNodeName())) {
            List<StateOption> stateOptions = new ArrayList<>();

            for (Object nodeObject : nodeList.getList()) {
                stateOptions.add(toChannelStateOption((NodeValue) nodeObject));
            }

            return stateOptions;
        }

        throw new ConversionException("Unknown type '" + nodeList.getNodeName() + "'!");
    }

    private StateOption toChannelStateOption(NodeValue nodeValue) throws ConversionException {
        if ("option".equals(nodeValue.getNodeName())) {
            final Map<String, String> attributes = nodeValue.getAttributes();
            final String value = Optional.ofNullable(attributes).map(entry -> entry.get("value"))
                    .orElseThrow(() -> new ConversionException("The node 'option' requires the attribute 'value'!"));
            return new StateOption(value, nodeValue.getValue().toString());
        }

        throw new ConversionException("Unknown type in the list of 'options'!");
    }

    @Override
    public final @Nullable Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Map<String, String> attributes = this.attributeMapValidator.readValidatedAttributes(reader);

        boolean readOnly = toBoolean(attributes, "readOnly", false);
        StateDescriptionFragmentBuilder builder = StateDescriptionFragmentBuilder.create().withReadOnly(readOnly);

        BigDecimal minimum = toBigDecimal(attributes, "min", null);
        if (minimum != null) {
            builder.withMinimum(minimum);
        }

        BigDecimal maximum = toBigDecimal(attributes, "max", null);
        if (maximum != null) {
            builder.withMaximum(maximum);
        }

        BigDecimal step = toBigDecimal(attributes, "step", null);
        if (step != null) {
            builder.withStep(step);
        }

        String pattern = attributes.get("pattern");
        if (pattern != null) {
            builder.withPattern(pattern);
        }

        NodeList nodes = (NodeList) context.convertAnother(context, NodeList.class);
        NodeIterator nodeIterator = new NodeIterator(nodes.getList());

        NodeList optionNodes = (NodeList) nodeIterator.next();
        if (optionNodes != null) {
            builder.withOptions(toListOfChannelState(optionNodes));
        }

        nodeIterator.assertEndOfType();

        return builder.build().toStateDescription();
    }
}
