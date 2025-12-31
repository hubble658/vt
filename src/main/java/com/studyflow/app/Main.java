package com.studyflow.app;

import com.studyflow.app.gui.StudyFlow;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Main {

	public static void main(String[] args) {
		Application.launch(StudyFlow.class, args);
	}

}
