/**
 * Copyright 2011 Matthias Nuessler <m.nuessler@web.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuessler.maven.plugin.cakupan;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.tools.ant.BuildException;

import com.cakupan.xslt.exception.XSLTCoverageException;
import com.cakupan.xslt.util.CoverageIOUtil;
import com.cakupan.xslt.util.XSLTCakupanUtil;

/**
 * Generate a test coverage report for XSLT files.
 * 
 * @author Matthias Nuessler
 * @goal cakupan
 * @execute phase="test" lifecycle="cakupan"
 */
public class CakupanReportMojo extends AbstractMavenReport {

    /**
     * <i>Maven Internal</i>: The Doxia Site Renderer.
     * 
     * @component
     */
    private Renderer siteRenderer;

    /**
     * <i>Maven Internal</i>: Project to interact with.
     * 
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The output directory for the report.
     * 
     * @parameter default-value="${project.reporting.outputDirectory}/cakupan"
     * @required
     */
    private File outputDirectory;

    /**
     * @parameter expression="${xslt.instrument.destdir}"
     *            default-value="${project.build.directory}/cakupan-instrument"
     */
    private File instrumentDestDir;

    public String getOutputName() {
        return "cakupan/dummy";
    }

    public String getName(Locale locale) {
        return getBundle(locale).getString("report.cakupan.name");
    }

    public String getDescription(Locale locale) {
        return getBundle(locale).getString("report.cakupan.description");
    }

    private ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle("cakupan-report", locale);
    }

    @Override
    protected Renderer getSiteRenderer() {
        return siteRenderer;
    }

    @Override
    protected String getOutputDirectory() {
        return outputDirectory.getAbsolutePath();
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    public boolean isExternalReport() {
        return true;
    }

    @Override
    public boolean canGenerateReport() {
        File instrumentationFile = new File(instrumentDestDir, "coverage.xml");
        if (!instrumentationFile.exists()) {
            getLog().warn(
                    "Can't generate Cakupan XSLT coverage report. Instrumentation file does not exist: "
                            + instrumentationFile);
            return false;
        }
        return true;
    }

    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        if (!canGenerateReport()) {
            return;
        }
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        new File(outputDirectory, "cakupan").mkdirs();
        getLog().info("Start Cakupan report mojo");
        getLog().info("report output dir: " + getOutputDirectory());

        File reportFile = new File(outputDirectory, "index.html");
        if (reportFile.exists()) {
            reportFile.delete();
        }

        try {
            FileUtils.copyFileToDirectory(new File(instrumentDestDir,
                    "coverage.xml"), outputDirectory);
        } catch (IOException e) {
            throw new MavenReportException(e.getMessage());
        }

        CoverageIOUtil.setDestDir(outputDirectory);
        try {
            XSLTCakupanUtil.generateCoverageReport();
        } catch (XSLTCoverageException e) {
            if (e.getRefId() == XSLTCoverageException.NO_COVERAGE_FILE) {
                getLog().error(
                        "No coverage files found in ["
                                + outputDirectory.getPath() + "]!");
            } else {
                throw new BuildException("Failed to make a coverage report!", e);
            }
        }
        getLog().info("End Cakupan report mojo");
    }

}
