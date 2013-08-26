package ru.andreymarkelov.atlas.plugins.requestedfiedls;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.search.QueryContextConverter;
import com.atlassian.jira.issue.customfields.searchers.ExactTextSearcher;
import com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper;
import com.atlassian.jira.issue.customfields.statistics.AbstractCustomFieldStatisticsMapper;
import com.atlassian.jira.issue.customfields.statistics.CustomFieldStattable;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.issue.search.LuceneFieldSorter;
import com.atlassian.jira.issue.search.searchers.renderer.SearchRenderer;
import com.atlassian.jira.issue.search.searchers.transformer.SearchInputTransformer;
import com.atlassian.jira.issue.statistics.StatisticsMapper;
import com.atlassian.jira.issue.statistics.TextFieldSorter;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.util.JqlSelectOptionsUtil;
import com.atlassian.jira.web.FieldVisibilityManager;

/**
 * Searcher.
 * 
 * @author Andrey Markelov
 */
public class SelectTextCustomFieldSearcher extends ExactTextSearcher implements CustomFieldStattable {
    private static class SortStringComparator implements Comparator<String> {
        public int compare(String s, String s1) {
            return s.compareTo(s1);
        }
    }

    private CustomField customField;

    private CustomFieldInputHelper customFieldInputHelper;

    private JqlOperandResolver jqlOperandResolver;

    private SearchInputTransformer searchInputTransformer;

    private SearchRenderer searchRenderer;

    /**
     * Constructor.
     */
    public SelectTextCustomFieldSearcher(
            JqlOperandResolver jqlOperandResolver,
            CustomFieldInputHelper customFieldInputHelper) {
        super(jqlOperandResolver, customFieldInputHelper);
        this.jqlOperandResolver = jqlOperandResolver;
        this.customFieldInputHelper = customFieldInputHelper;
    }

    private List<FieldConfig> getConfigs(CustomField field) {
        List<FieldConfig> configs = new ArrayList<FieldConfig>();
        for (FieldConfigScheme cs : field.getConfigurationSchemes()) {
            configs.addAll(cs.getConfigs().values());
        }
        return configs;
    }

    @Override
    public SearchInputTransformer getSearchInputTransformer() {
        return searchInputTransformer;
    }

    @Override
    public SearchRenderer getSearchRenderer() {
        return searchRenderer;
    }

    @Override
    public LuceneFieldSorter<String> getSorter(final CustomField customField) {
        return new TextFieldSorter(customField.getId()) {
            @Override
            public Comparator<String> getComparator() {
                return new SortStringComparator();
            }
        };
    }

    public StatisticsMapper getStatisticsMapper(CustomField customField) {
        return new AbstractCustomFieldStatisticsMapper(customField) {
            @Override
            public Comparator getComparator() {
                return new Comparator() {
                    public int compare(Object o1, Object o2)
                    {
                        if (o1 == null && o2 == null) {
                            return 0;
                        } else if (o1 == null) {
                            return 1;
                        } else if (o2 == null) {
                            return -1;
                        }

                        return ((String) o1).compareTo((String) o2);
                    }
                };
            }

            @Override
            protected String getSearchValue(Object o) {
                if( o == null ) {
                    return null;
                } else {
                    return o.toString();
                }
            }

            public Object getValueFromLuceneField(String id) {
                if(id != null) {
                    return id;
                } else {
                    return null;
                }
            }
        };
    }

    @Override
    public void init(CustomField field) {
        customField = field;

        ClauseNames clauseNames = customField.getClauseNames();
        JqlSelectOptionsUtil jqlSelectOptionsUtil = ComponentManager.getComponentInstanceOfType(JqlSelectOptionsUtil.class);
        QueryContextConverter queryContextConverter = new QueryContextConverter();
        FieldVisibilityManager fieldVisibilityManager = ComponentManager.getComponentInstanceOfType(FieldVisibilityManager.class);

        boolean isXmlField = field.getCustomFieldType().getKey().equals("ru.andreymarkelov.atlas.plugins.requestedfields:xml-request-custom-field");

        searchInputTransformer = new SelectTextCustomFieldSearchInputTransformer(
            customField.getId(),
            clauseNames,
            field,
            jqlOperandResolver,
            jqlSelectOptionsUtil,
            queryContextConverter,
            customFieldInputHelper);

        searchRenderer = new SelectTextCustomFieldRenderer(
            clauseNames,
            getDescriptor(),
            customField,
            new SelectTextCustomFieldValueProvider(getConfigs(field), isXmlField),
            fieldVisibilityManager);

        super.init(field);
    }
}