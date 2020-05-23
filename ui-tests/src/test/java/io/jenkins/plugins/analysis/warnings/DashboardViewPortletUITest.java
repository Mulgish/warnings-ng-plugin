package io.jenkins.plugins.analysis.warnings;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.plugins.dashboard_view.DashboardView;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.jenkinsci.test.acceptance.po.Job;
import io.jenkins.plugins.analysis.warnings.DashboardTable.DashboardTableEntry;
import static io.jenkins.plugins.analysis.warnings.Assertions.*;

/**
 * Integration tests for the dashboard portlet.
 *
 * @author Lukas Kirner
 */
public class DashboardViewPortletUITest extends AbstractJUnitTest {
    private static final String WARNINGS_PLUGIN_PREFIX = "/";

    @Test
    public void shouldShowIcons() {
        DashboardView dashboardView = createDashboardWithStaticAnalysisPortlet(false, true);
        FreeStyleJob job = createFreeStyleJob("issue_filter/checkstyle-result.xml");
        job.addPublisher(IssuesRecorder.class, recorder -> recorder.setTool("CheckStyle"));
        job.save();
        Build build = shouldBuildJobSuccessfully(job);

        DashboardTable dashboardTable = new DashboardTable(build, dashboardView.url, "portlet-topPortlets-" + 0);

        List<String> headers = dashboardTable.headers;
        assertThat(headers.get(0)).contains("Job");
        assertThat(headers.get(1)).contains("");

        Map<String, Map<String, DashboardTableEntry>> table = dashboardTable.table;
        assertThat(table.get(job.name).get("").getWarningsCount()).isEqualTo(4);
    }

    @Test
    public void shouldNotShowIcons() {
        DashboardView dashboardView = createDashboardWithStaticAnalysisPortlet(false, false);
        FreeStyleJob job = createFreeStyleJob("issue_filter/checkstyle-result.xml");
        job.addPublisher(IssuesRecorder.class, recorder -> recorder.setTool("CheckStyle"));
        job.save();
        Build build = shouldBuildJobSuccessfully(job);

        DashboardTable dashboardTable = new DashboardTable(build, dashboardView.url, "portlet-topPortlets-" + 0);

        List<String> headers = dashboardTable.headers;
        assertThat(headers.get(0)).contains("Job");
        assertThat(headers.get(1)).contains("CheckStyle");

        Map<String, Map<String, DashboardTableEntry>> table = dashboardTable.table;
        assertThat(table.get(job.name).get("CheckStyle").getWarningsCount()).isEqualTo(4);
    }

    @Test
    public void shouldNotShowCleanJobOnHideClean() {
        DashboardView dashboardView = createDashboardWithStaticAnalysisPortlet(true, false);
        FreeStyleJob job = createFreeStyleJob("issue_filter/checkstyle-clean.xml");
        job.addPublisher(IssuesRecorder.class, recorder -> recorder.setTool("CheckStyle"));
        job.save();
        Build build = shouldBuildJobSuccessfully(job);

        DashboardTable dashboardTable = new DashboardTable(build, dashboardView.url, "portlet-topPortlets-" + 0);

        assertThat(dashboardTable.headers).isEmpty();
        assertThat(dashboardTable.table).isEmpty();
    }

    private Build shouldBuildJobSuccessfully(final Job job) {
        Build build = job.startBuild().waitUntilFinished();
        assertThat(build.isSuccess()).isTrue();
        return build;
    }

    private FreeStyleJob createFreeStyleJob(final String... resourcesToCopy) {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        ScrollerUtil.hideScrollerTabBar(driver);
        for (String resource : resourcesToCopy) {
            job.copyResource(WARNINGS_PLUGIN_PREFIX + resource);
        }
        return job;
    }

    private DashboardView createDashboardWithStaticAnalysisPortlet(final Boolean hideCleanJobs, final Boolean showIcons) {
        DashboardView v = createDashboardView();
        StaticAnalysisIssuesPerToolAndJobPortlet portlet = v.addTopPortlet(StaticAnalysisIssuesPerToolAndJobPortlet.class);
        if (hideCleanJobs) { portlet.toggleHideCleanJobs(); }
        if (showIcons) { portlet.toggleShowIcons(); }
        v.save();
        return v;
    }

    private DashboardView createDashboardView() {
        DashboardView v = jenkins.views.create(DashboardView.class);
        v.configure();
        v.matchAllJobs();
        return v;
    }
}
