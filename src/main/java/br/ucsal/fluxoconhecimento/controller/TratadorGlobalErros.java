package br.ucsal.fluxoconhecimento.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import br.ucsal.fluxoconhecimento.dto.ErroDTO;
import br.ucsal.fluxoconhecimento.excecao.AcessoNegadoException;
import br.ucsal.fluxoconhecimento.excecao.FalhaAutenticacaoException;
import br.ucsal.fluxoconhecimento.excecao.RecursoNaoEncontradoException;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;

// Transforma os erros da API em JSON com mensagem em português (RNF12, RNF13)
@RestControllerAdvice
public class TratadorGlobalErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalErros.class);

    // Bean Validation nos DTOs (ex.: título em branco - RN04)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroDTO> validacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        return resposta(HttpStatus.BAD_REQUEST, "Alguns campos precisam ser corrigidos.", campos);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroDTO> regraNegocio(RegraNegocioException ex) {
        Map<String, String> campos = ex.getCampo() == null ? Map.of() : Map.of(ex.getCampo(), ex.getMessage());
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), campos);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroDTO> naoEncontrado(RecursoNaoEncontradoException ex) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroDTO> acessoNegado(AcessoNegadoException ex) {
        return resposta(HttpStatus.FORBIDDEN, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(FalhaAutenticacaoException.class)
    public ResponseEntity<ErroDTO> falhaAutenticacao(FalhaAutenticacaoException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroDTO> corpoInvalido(HttpMessageNotReadableException ex) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Os dados enviados estão em um formato inválido. Confira os campos e tente novamente.", Map.of());
    }

    // RF16 - arquivo acima do limite
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroDTO> arquivoGrande(MaxUploadSizeExceededException ex) {
        return resposta(HttpStatus.PAYLOAD_TOO_LARGE, "O arquivo pode ter no máximo 5 MB.", Map.of("arquivo", "O arquivo pode ter no máximo 5 MB."));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErroDTO> arquivoAusente(MissingServletRequestPartException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Escolha um arquivo para anexar.", Map.of("arquivo", "Escolha um arquivo para anexar."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroDTO> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "O valor informado em \"" + ex.getName() + "\" é inválido.", Map.of());
    }

    // Erro inesperado: mensagem genérica e registro no log
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroDTO> inesperado(Exception ex) throws Exception {
        if (ex instanceof ErrorResponse) {
            throw ex;
        }
        log.error("Erro inesperado", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente; se continuar, avise o administrador.", Map.of());
    }

    private static ResponseEntity<ErroDTO> resposta(HttpStatus status, String mensagem, Map<String, String> campos) {
        return ResponseEntity.status(status).body(new ErroDTO(status.value(), mensagem, campos));
    }
}
