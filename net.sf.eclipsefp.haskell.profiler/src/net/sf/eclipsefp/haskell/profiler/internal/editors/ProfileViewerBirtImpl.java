/**
 * (c) 2011, Alejandro Serrano
 * Released under the terms of the EPL.
 */
package net.sf.eclipsefp.haskell.profiler.internal.editors;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import net.sf.eclipsefp.haskell.profiler.ProfilerPlugin;
import net.sf.eclipsefp.haskell.profiler.internal.util.UITexts;
import net.sf.eclipsefp.haskell.profiler.model.Job;
import net.sf.eclipsefp.haskell.profiler.model.Sample;

import org.eclipse.birt.chart.model.Chart;
import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.attribute.LegendItemType;
import org.eclipse.birt.chart.model.attribute.TickStyle;
import org.eclipse.birt.chart.model.component.Axis;
import org.eclipse.birt.chart.model.component.Series;
import org.eclipse.birt.chart.model.component.impl.SeriesImpl;
import org.eclipse.birt.chart.model.data.NumberDataSet;
import org.eclipse.birt.chart.model.data.SeriesDefinition;
import org.eclipse.birt.chart.model.data.impl.NumberDataSetImpl;
import org.eclipse.birt.chart.model.data.impl.SeriesDefinitionImpl;
import org.eclipse.birt.chart.model.impl.ChartWithAxesImpl;
import org.eclipse.birt.chart.model.type.AreaSeries;
import org.eclipse.birt.chart.model.type.impl.AreaSeriesImpl;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ContainerGenerator;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.FileStoreEditorInput;

/**
 * Viewer for profiling output generated by GHC.
 * Uses the BIRT Charting engine.
 * @author Alejandro Serrano
 */
public abstract class ProfileViewerBirtImpl extends ProfileViewerImpl{
	static int INITIAL_NUMBER_OF_ITEMS = 15;

	Job job = null;
	double[] samplePoints;
	List<Map.Entry<String, BigInteger>> entries;
	
	private FormToolkit toolkit;
	private ScrolledForm form;
	private Scale slider;
	private ChartCanvas canvas;
	private IEditorInput input;

	private String getFileContents(IEditorInput input) throws Exception{
		InputStream contents=null;
		try {
			if (input instanceof FileStoreEditorInput) {
				FileStoreEditorInput fInput = (FileStoreEditorInput) input;
				URI path = fInput.getURI();
				contents = new FileInputStream(new File(path));
				setPartName(fInput.getName());
			} else {
				IFileEditorInput fInput = (IFileEditorInput) input;
				IFile inputFile = fInput.getFile();
				setPartName(inputFile.getName());
				contents = inputFile.getContents();
			}
			return new Scanner(contents).useDelimiter("\\Z").next();
		} finally {
			contents.close();
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		this.input=input;
		try {
			job = Job.parse(new StringReader(getFileContents(input)));
			// Sort entries
			entries = job.sortEntriesByTotal();
			// Get sample points
			samplePoints = new double[job.getSamplesAndTimes().size()];
			int i = 0;
			for (Sample s : job.getSamples()) {
				// #158: round times
				samplePoints[i] = new BigDecimal(new Float(s.getTime()).doubleValue()).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
				i++;
			}

		} catch (Exception e) {
			ProfilerPlugin.log(IStatus.ERROR, e.getLocalizedMessage(), e);
			throw new PartInitException(Status.CANCEL_STATUS);
		}
	}
	
	@Override
	public void createPartControl(Composite parent) {
		
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createScrolledForm(parent);
		form.setText(NLS.bind(UITexts.graph_title, new Object[] { job.getName(), job.getDate() }));
		
		int n = entries.size() < INITIAL_NUMBER_OF_ITEMS ? entries.size() : INITIAL_NUMBER_OF_ITEMS;
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		form.getBody().setLayout(layout);
		
		final Chart chart = createChart();
		populateChart(chart, n);
		canvas = new ChartCanvas(form.getBody(), SWT.NONE);
		canvas.setLayoutData(new GridData(GridData.FILL_BOTH));
		canvas.setChart(chart);
		
		Section section = toolkit.createSection(form.getBody(), 
				ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE);
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		section.setText(UITexts.graph_options);
		
		Composite sectionClient = toolkit.createComposite(section);
		GridLayout hLayout = new GridLayout();
		hLayout.numColumns = 2;
		sectionClient.setLayout(hLayout);
		section.setClient(sectionClient);

		toolkit.createLabel(sectionClient, UITexts.graph_ungroupElements);
		slider = new Scale(sectionClient, SWT.NONE);
		slider.setMinimum(0);
		slider.setMaximum(entries.size());
		slider.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		slider.setSelection(n);
		slider.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				populateChart(chart, slider.getSelection());
				canvas.rebuildChart();
				canvas.redraw();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				populateChart(chart, slider.getSelection());
				canvas.rebuildChart();
				canvas.redraw();
			}
		});

		toolkit.paintBordersFor(parent);
	}

	private Chart createChart(){
		Chart chart = ChartWithAxesImpl.create();
		// Title
		// chart.getTitle().getLabel().getCaption().setValue(job.getName());
		chart.getTitle().setVisible(false);
		// chart.getTitle().getLabel().getCaption().getFont().setSize(14);
		// chart.getTitle().getLabel().getCaption().getFont().setName("Arial");
		// Legend
		chart.getLegend().setItemType(LegendItemType.SERIES_LITERAL);
		chart.getLegend().setVisible(true);
		// X-Axis -> time
		Axis xAxis = ((ChartWithAxes) chart).getPrimaryBaseAxes()[0];
		// xAxis.setType(AxisType.LINEAR_LITERAL);
		xAxis.getMajorGrid().setTickStyle(TickStyle.BELOW_LITERAL);
		xAxis.getTitle().setVisible(true);
		xAxis.getTitle().getCaption().setValue(job.getSampleUnit());
		xAxis.getLabel().setVisible(true);
		// X-Axis data
		NumberDataSet xDataSet = NumberDataSetImpl.create(samplePoints);
		Series xCategory = SeriesImpl.create();
		xCategory.setDataSet(xDataSet);
		SeriesDefinition sdX = SeriesDefinitionImpl.create();
		sdX.getSeriesPalette().shift(0);
		xAxis.getSeriesDefinitions().add(sdX);
		sdX.getSeries().add(xCategory);
		// Y-Axis -> memory
		Axis yAxis = ((ChartWithAxes) chart).getPrimaryOrthogonalAxis(xAxis);
		yAxis.getMajorGrid().setTickStyle(TickStyle.LEFT_LITERAL);
		yAxis.getMajorGrid().getLineAttributes().setVisible(true);
		yAxis.getMinorGrid().getLineAttributes().setVisible(true);
		yAxis.setPercent(false);
		yAxis.getTitle().getCaption().setValue(job.getValueUnit());
		yAxis.getTitle().setVisible(true);
		yAxis.getTitle().getCaption().getFont().setRotation(90);
		yAxis.getLabel().setVisible(true);
		// Y-Axis data
		SeriesDefinition sdY = SeriesDefinitionImpl.create();
		sdY.getSeriesPalette().shift(1);
		yAxis.getSeriesDefinitions().add(sdY);
		return chart;
	}
	
	private void populateChart(Chart chart,int numberApart) {
		int n = entries.size() < numberApart ? entries.size() : numberApart;
		List<Map.Entry<String, BigInteger>> entriesApart = new ArrayList<>(
				entries.subList(0, n));
		Axis xAxis = ((ChartWithAxes) chart).getPrimaryBaseAxes()[0];
		Axis yAxis = ((ChartWithAxes) chart).getPrimaryOrthogonalAxis(xAxis);
		SeriesDefinition sdY=yAxis.getSeriesDefinitions().get(0);
		sdY.getSeries().clear();
		// Get the numbers
		ProfileNumbers numbers = new ProfileNumbers(entriesApart, samplePoints.length);
		numbers.fillIn(job);
		// Add (rest) elements
		NumberDataSet restDataSet = NumberDataSetImpl.create(numbers.getRest());
		AreaSeries restSeries = (AreaSeries) AreaSeriesImpl.create();
		restSeries.setSeriesIdentifier(UITexts.graph_restOfTrace);
		restSeries.setDataSet(restDataSet);
		restSeries.getLineAttributes().setVisible(false);
		restSeries.getLabel().setVisible(false);
		restSeries.setStacked(true);
		sdY.getSeries().add(restSeries);
		// Add apart elements, in reverse order
		Collections.reverse(entriesApart);
		for (Map.Entry<String, BigInteger> entry : entriesApart) {
			double[] entryNumbers = numbers.getEntries().get(entry.getKey());
			NumberDataSet entryDataSet = NumberDataSetImpl.create(entryNumbers);
			AreaSeries entrySeries = (AreaSeries) AreaSeriesImpl.create();
			entrySeries.setSeriesIdentifier(entry.getKey());
			entrySeries.setDataSet(entryDataSet);
			entrySeries.getLineAttributes().setVisible(false);
			entrySeries.getLabel().setVisible(false);
			entrySeries.setStacked(true);
			sdY.getSeries().add(entrySeries);
		}

	}
	
	@Override
	public void doSaveAs(Shell shell,String partName) {
		SaveAsDialog dialog = new SaveAsDialog(shell);
		dialog.setOriginalName(partName);
		if (dialog.open() == Window.OK) {
			try {
				IPath path = dialog.getResult();
				IPath folder = path.uptoSegment(path.segmentCount() - 1);
				ContainerGenerator gen = new ContainerGenerator(folder);
				IContainer con = gen.generateContainer(null);
				IFile file = con.getFile(Path.fromPortableString(path.lastSegment()));
				byte[] bytes = getFileContents(input).getBytes();
				try (InputStream source = new ByteArrayInputStream(bytes)) {
				    if (!file.exists()) {
				        file.create(source, IResource.NONE, null);
				    } else {
				        file.setContents(source, IResource.NONE, null);
				   }
				}
				setPartName(path.lastSegment());
			} catch (Exception e) {
				// Do nothing
			}
		}
	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}
}