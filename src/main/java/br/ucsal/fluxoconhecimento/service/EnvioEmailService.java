package br.ucsal.fluxoconhecimento.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Envio de e-mails (RF14)
@Service
public class EnvioEmailService {

    private static final Logger log = LoggerFactory.getLogger(EnvioEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remetente;

    public EnvioEmailService(ObjectProvider<JavaMailSender> mailSender,
                             @Value("${knowflow.email.remetente}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    public void enviar(String para, String assunto, String texto) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("""

                    ===== E-MAIL (servidor SMTP nao configurado; mostrado apenas no log) =====
                    Para: {}
                    Assunto: {}

                    {}
                    =========================================================================""", para, assunto, texto);
            return;
        }
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(para);
        mensagem.setSubject(assunto);
        mensagem.setText(texto);
        try {
            sender.send(mensagem);
        } catch (MailException e) {
            // Não revela ao usuário se o e-mail existe; apenas registra a falha
            log.error("Falha ao enviar e-mail para {}", para, e);
        }
    }
}
