package com.atlassian.jira.web.filters.steps.security;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.module.ModuleFactory;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.List;

import static org.junit.Assert.assertThat;

public class PathExclusionModuleDescriptorTest {
    private static final String SAMPLE_EXCLUSION_PATH_ENTRIES = "<clickjacking-http-headers-excluded-paths key=\"charlie-unit-test-exclusion\">\n" +
            " <path>/charlie-path-01</path>\n" +
            " <path>/charlie-path-10</path>\n" +
            "</clickjacking-http-headers-excluded-paths>";

    private static final String NO_PATHS_ENTRIES = "<clickjacking-http-headers-excluded-paths key=\"charlie-unit-test-exclusion\">\n" +
            "</clickjacking-http-headers-excluded-paths>";

    private static final String WHITESPACE_PATH_ENTRIES = "<clickjacking-http-headers-excluded-paths key=\"charlie-unit-test-exclusion\">\n" +
            " <path></path>\n"  +
            " <path> </path>\n"  +
            " <path>  </path>\n"  +
            "</clickjacking-http-headers-excluded-paths>";

    @Rule
    public MethodRule initMockito = MockitoJUnit.rule();
    @Mock
    private ModuleFactory moduleFactory;
    @Mock
    private Plugin plugin;

    private PathExclusionModuleDescriptor moduleDescriptor;

    @Before
    public void setUp() {
        moduleDescriptor = new PathExclusionModuleDescriptor(moduleFactory);
    }

    @Test
    public void shouldProperlyParseBasicParametersFromXml() {
        moduleDescriptor.init(plugin, toElement(SAMPLE_EXCLUSION_PATH_ENTRIES));
        PathExclusion module = moduleDescriptor.getModule();

        final List<String> excludedPaths = module.getExcludedPaths();
        assertThat(excludedPaths, Matchers.containsInAnyOrder("/charlie-path-01", "/charlie-path-10"));
    }

    @Test
    public void shouldWorkProperlyWhenNoPathEntires() {
        moduleDescriptor.init(plugin, toElement(NO_PATHS_ENTRIES));
        PathExclusion module = moduleDescriptor.getModule();

        final List<String> excludedPaths = module.getExcludedPaths();
        assertThat(excludedPaths, Matchers.empty());
    }

    @Test
    public void shouldFilterOutWhitespaceEntires() {
        moduleDescriptor.init(plugin, toElement(WHITESPACE_PATH_ENTRIES));
        PathExclusion module = moduleDescriptor.getModule();

        final List<String> excludedPaths = module.getExcludedPaths();
        assertThat(excludedPaths, Matchers.empty());
    }


    private static Element toElement(String xml) {
        final SAXReader saxReader = new SAXReader();
        try {
            final Document document = saxReader.read(IOUtils.toInputStream(xml));
            return document.getRootElement();

        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }
}
