package com.hampcode.ticketsales.agent.app;

import com.hampcode.ticketsales.agent.app.dto.AgentResponse;
import com.hampcode.ticketsales.agent.app.dto.VoiceResponse;
import com.hampcode.ticketsales.agent.app.tools.PurchaseTools;
import com.hampcode.ticketsales.agent.core.AgentService;
import com.hampcode.ticketsales.agent.core.voice.VoiceService;
import com.hampcode.ticketsales.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.voice.enabled", havingValue = "true", matchIfMissing = true)
public class VoiceController {

    static final String PENDING_PURCHASE_MESSAGE = "Tienes una compra pendiente. Confírmala o cancélala con los botones.";

    private final VoiceService voiceService;
    private final AgentService agentService;
    private final PurchaseTools purchaseTools;

    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VoiceResponse voice(
            @RequestParam MultipartFile audio,
            @RequestParam(required = false) String conversationId,
            @RequestParam(defaultValue = "true") boolean speak,
            Authentication auth) throws IOException {
        if (audio.isEmpty()) throw new InvalidRequestException("No llegó ningún audio");

        String transcript = voiceService.transcribe(audio.getBytes(), audio.getOriginalFilename());
        if (VoiceService.isNoise(transcript)) throw new InvalidRequestException("No pude entender el audio, intenta de nuevo");

        AgentResponse response = purchaseTools.hasPending(auth.getName())
                ? AgentResponse.onlyMessage(conversationId, PENDING_PURCHASE_MESSAGE)
                : agentService.chat(auth.getName(), roles(auth), transcript, conversationId);

        byte[] spoken = speak ? voiceService.speak(response.message()) : null;
        return new VoiceResponse(transcript, response, spoken);
    }

    private Set<String> roles(Authentication auth) {
        return auth.getAuthorities().stream().map(a -> a.getAuthority().replace("ROLE_", "")).collect(Collectors.toSet());
    }
}
