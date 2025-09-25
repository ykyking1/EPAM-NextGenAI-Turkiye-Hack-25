//package com.burak.openai.controller;
//
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import reactor.core.publisher.Flux;
//
//@RestController
//@RequestMapping("/api")
//public class StreamController {
//
//	private final ChatClient chatClient;
//
//	public StreamController(@Qualifier() ChatClient chatClient) {
//		this.chatClient = chatClient;
//	}
//
//	@GetMapping("/stream")
//	public Flux<String> stream(@RequestParam("message") String message) {
//		return chatClient.prompt().user(message).stream().content();
//	}
//}