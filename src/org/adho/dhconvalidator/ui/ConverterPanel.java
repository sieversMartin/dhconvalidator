package org.adho.dhconvalidator.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.adho.dhconvalidator.conftool.User;
import org.adho.dhconvalidator.conversion.ConversionPath;
import org.adho.dhconvalidator.conversion.Converter;
import org.adho.dhconvalidator.conversion.oxgarage.ZipResult;
import org.adho.dhconvalidator.properties.PropertyKey;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.BrowserWindowOpener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.Page;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.FailedListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.themes.BaseTheme;
import com.vaadin.ui.VerticalLayout;

public class ConverterPanel extends VerticalLayout implements View {
	
	private static final Logger LOGGER = Logger.getLogger(ConverterPanel.class.getName());
	
	private Upload upload;
	private ByteArrayOutputStream uploadContent;
	private String filename;
	private ProgressBar progressBar;
	private Label logArea;
	private Label preview;
	private HorizontalSplitPanel resultPanel;
	private Label resultCaption;
	private Button btDownloadResult;
	private Label downloadInfo;

	private FileDownloader currentFileDownloader;
	
	public ConverterPanel() {
		initComponents();
		initActions();
	}

	private void initActions() {
		upload.addSucceededListener(new SucceededListener() {
			
			@Override
			public void uploadSucceeded(SucceededEvent event) {
				try {
					byte[] uploadData = uploadContent.toByteArray();
					if (uploadData.length == 0) {
						Notification.show(
							"Info", 
							"Please select a file first!", 
							Type.TRAY_NOTIFICATION);
					}
					else {
						appendLogMessage("Starting conversion...");
						Converter converter =
								new Converter(
									PropertyKey.oxgarage_url.getValue());
						
						ZipResult zipResult = converter.convert(
							uploadData, 
							ConversionPath.getConvertionPathByFilename(filename),
							(User)VaadinSession.getCurrent().getAttribute(
									SessionStorageKey.USER.name()),
							filename);
						appendLogMessage("Finished conversion.");
						VaadinSession.getCurrent().setAttribute(
								SessionStorageKey.ZIPRESULT.name(), zipResult);
						System.out.println(converter.getContentAsXhtml());
						appendLogMessage("Result rendered in preview");
						
						preview.setValue(converter.getContentAsXhtml());
						resultCaption.setValue("Preview and Conversion log for " + filename );
						prepareForResultDownload();
					}
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "error converting document", e);
					appendLogMessage("ERROR: " + e.getMessage());
				}
				progressBar.setVisible(false);
				UI.getCurrent().setPollInterval(-1);	
			}
		});
		upload.addStartedListener(new StartedListener() {
			
			@Override
			public void uploadStarted(StartedEvent event) {
				preview.setValue("");
				logArea.setValue("");
				btDownloadResult.setVisible(false);
				downloadInfo.setVisible(false);
				UI.getCurrent().push();
				
				progressBar.setVisible(true);
				UI.getCurrent().setPollInterval(500);
			}
		});

		upload.addFailedListener(new FailedListener() {
			
			@Override
			public void uploadFailed(FailedEvent event) {
				resultCaption.setValue("Preview and Conversion log for file " + filename );
				progressBar.setVisible(false);
				UI.getCurrent().setPollInterval(-1);
			}
		});
	}
	
	private void prepareForResultDownload() {
		downloadInfo.setVisible(true);
		
		if (currentFileDownloader != null) {
			currentFileDownloader.remove();
		}
		
		currentFileDownloader = new FileDownloader(new StreamResource(
				new StreamSource() {
			
					@Override
					public InputStream getStream() {
						return createResultStream();
					}
				}, filename.substring(0, filename.lastIndexOf('.')) + ".dhc" ));
		currentFileDownloader.extend(btDownloadResult);
		
		btDownloadResult.setVisible(true);
	}

	private void appendLogMessage(String logmesssage) {
		StringBuilder logBuilder = 
			new StringBuilder(
				(logArea.getValue()==null)?"":logArea.getValue());
		
		logBuilder.append("<br>");
		logBuilder.append(logmesssage);
		logArea.setReadOnly(false);
		logArea.setValue(logBuilder.toString());
		logArea.setReadOnly(true);
		UI.getCurrent().push();
	}

	private void initComponents() {
		setMargin(true);
		setSizeFull();
		setSpacing(true);
		HorizontalLayout headerPanel = new HorizontalLayout();
		headerPanel.setWidth("100%");
		Label title = new Label("DHConvalidator");
		title.addStyleName("title-caption");
		headerPanel.addComponent(title);
		headerPanel.setComponentAlignment(title, Alignment.TOP_LEFT);
		LogoutLink logoutLink = new LogoutLink();
		headerPanel.addComponent(logoutLink);
		headerPanel.setComponentAlignment(logoutLink, Alignment.TOP_RIGHT);
		addComponent(headerPanel);
		
		HorizontalLayout inputPanel = new HorizontalLayout();
		inputPanel.setSpacing(true);
		addComponent(inputPanel);
		
		upload = new Upload(
			"Please upload your .docx or .odt file",
			new Receiver() {
				@Override
				public OutputStream receiveUpload(String filename,
						String mimeType) {
					ConverterPanel.this.filename = filename;
					ConverterPanel.this.uploadContent = new ByteArrayOutputStream(); 
					return ConverterPanel.this.uploadContent;
				}
			});

		inputPanel.addComponent(upload);
		
		progressBar = new ProgressBar();
		progressBar.setIndeterminate(true);
		progressBar.setVisible(false);
		inputPanel.addComponent(progressBar);
		inputPanel.setComponentAlignment(progressBar, Alignment.MIDDLE_CENTER);
		progressBar.addStyleName("converterpanel-progressbar");
	
		resultCaption = new Label("Preview and Conversion log");
		resultCaption.setWidth("100%");
		resultCaption.addStyleName("converterpanel-resultcaption");
		addComponent(resultCaption);
		setComponentAlignment(resultCaption, Alignment.MIDDLE_CENTER);
		
		resultPanel = new HorizontalSplitPanel();
		addComponent(resultPanel);
		resultPanel.setSizeFull();
		setExpandRatio(resultPanel, 1.0f);
		
		preview = new Label("", ContentMode.HTML);
		preview.addStyleName("tei-preview");
		resultPanel.addComponent(preview);
		VerticalLayout rightPanel = new VerticalLayout();
		rightPanel.setMargin(new MarginInfo(false, false, true, true));
		rightPanel.setSpacing(true);
		resultPanel.addComponent(rightPanel);
		
		logArea = new Label("", ContentMode.HTML);
		logArea.setSizeFull();
		logArea.setReadOnly(true);
		rightPanel.addComponent(logArea);
		
		
		downloadInfo = new Label("If the preview looks like what you "
				+ "meant hit the 'Download result' button "
				+ "and upload the ZIP file to ConfTool:");
		rightPanel.addComponent(downloadInfo);
		downloadInfo.setVisible(false);
		
		btDownloadResult = new Button("Download result");
		btDownloadResult.setVisible(false);
		rightPanel.addComponent(btDownloadResult);
		rightPanel.setComponentAlignment(btDownloadResult, Alignment.BOTTOM_CENTER);
		btDownloadResult.setHeight("50px");
		
		rightPanel.addComponent(new Label("If you are unsure about what the preview "
				+ "should look like have a look at our example submission: "));
		Button btExample = new Button("Take me to the example!");
		btExample.setStyleName(BaseTheme.BUTTON_LINK);
		btExample.addStyleName("plain-link");
		rightPanel.addComponent(btExample);

		new BrowserWindowOpener(DHConvalidatorExample.class).extend(btExample);
	}
	
	private InputStream createResultStream() {
		try {
			ZipResult result = (ZipResult) VaadinSession.getCurrent().getAttribute(
					SessionStorageKey.ZIPRESULT.name());
			return new ByteArrayInputStream(result.toZipData());
		} catch (IOException e) {
			e.printStackTrace();
			Notification.show(
					"Error", 
					"result creation failed",
					Type.ERROR_MESSAGE);
			return null;
		}
	}

	@Override
	public void enter(ViewChangeEvent event) {
		VaadinSession.getCurrent().setAttribute(
				SessionStorageKey.ZIPRESULT.name(), null);	
		btDownloadResult.setVisible(false);
		downloadInfo.setVisible(false);
		preview.setValue("");
		logArea.setValue("");
		resultCaption.setValue("Preview and Conversion log");
		Page.getCurrent().setTitle("DHConvalidator Conversion and Validation");
	}

}
