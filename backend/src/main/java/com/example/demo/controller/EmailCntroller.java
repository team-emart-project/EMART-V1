package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.File;

import jakarta.mail.internet.MimeMessage;

@RestController
public class EmailCntroller {

	private final JavaMailSender mailSender;

	public EmailCntroller(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

//	@RequestMapping("/send-email")
//	public String sendEmail() {
//		try {
//			SimpleMailMessage message = new SimpleMailMessage();
//			message.setSubject("regarding order");
//			message.setFrom("keniv935@gmail.com");
//			message.setTo("vaibhavkeni450@gmail.com");
//			message.setText("Your order successfully placed!");
//			mailSender.send(message);
//			return "success";
//		} catch (Exception e) {
//			return e.getMessage();
//		}
//	}

	@RequestMapping("/send-attachment")
	public String sendAttachement() {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setSubject("regarding order with attachment");
			helper.setFrom("keniv935@gmail.com");
			helper.setTo("vaibhavkeni450@gmail.com");
			helper.setText("find attachment");
			helper.addAttachment("e-mart-DB-Documentation v1.0.xlsx",new File("C:\\Users\\Vaibhav\\Downloads\\e-mart-DB-Documentation v1.0.xlsx"));
			mailSender.send(message);
			return "Success"; 
		} catch (Exception e) {
			return e.getMessage();
		}
	}
}