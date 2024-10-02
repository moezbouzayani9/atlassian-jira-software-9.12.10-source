package com.atlassian.jira.security.serialization;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.module.ModuleFactory;
import org.dom4j.Element;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestXmlPluginAllowlistModuleDescriptor {

    @Mock
    private ModuleFactory moduleFactory;

    @InjectMocks
    private XmlPluginAllowlistModuleDescriptor descriptor;

    @Test
    void notInitialized() {
        assertThat(descriptor.getModule(), Matchers.nullValue());
    }

    static class Provider implements XmlPluginAllowlistProvider {

        @Override
        public Set<String> getAllowlistedClasses() {
            return new HashSet<>();
        }
    }

    @Mock
    private Plugin plugin;
    @Mock
    private Element element;

    @Test
    void initializedAndEnabled() {
        Provider provider = new Provider();
        lenient().when(element.attributeValue("class")).thenReturn(Provider.class.getName());
        when(moduleFactory.createModule(Provider.class.getName(), descriptor)).thenReturn(provider);

        descriptor.init(plugin, element);
        descriptor.enabled();

        assertThat(descriptor.getModule(), Matchers.sameInstance(provider));
    }
}