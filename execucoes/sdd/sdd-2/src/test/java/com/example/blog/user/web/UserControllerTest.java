package com.example.blog.user.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.blog.shared.config.ClockConfig;
import com.example.blog.shared.error.ConflictException;
import com.example.blog.shared.error.GlobalExceptionHandler;
import com.example.blog.shared.error.ValidationException;
import com.example.blog.shared.security.CurrentUserId;
import com.example.blog.shared.security.JwtConfig;
import com.example.blog.shared.security.SecurityConfig;
import com.example.blog.shared.security.TokenAuthenticationEntryPoint;
import com.example.blog.shared.security.TokenAuthorizationHeaderResolver;
import com.example.blog.user.service.AuthenticatedUser;
import com.example.blog.user.service.UserService;
import com.example.blog.user.service.UserUpdate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtConfig.class, ClockConfig.class, TokenAuthorizationHeaderResolver.class,
        TokenAuthenticationEntryPoint.class, CurrentUserId.class, GlobalExceptionHandler.class})
class UserControllerTest {

    private static final AuthenticatedUser JACOB =
            new AuthenticatedUser(7L, "jake@jake.jake", "Jacob", null, null, "jwt-ficticio");

    private static final String USER_JACOB = """
            {"user":{"email":"jake@jake.jake","token":"jwt-ficticio","username":"Jacob","bio":null,"image":null}}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void deveResponder201ComUserNoCadastro() throws Exception {
        when(userService.cadastrar("Jacob", "jake@jake.jake", "jakejake")).thenReturn(JACOB);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"user":{"username":"Jacob","email":"jake@jake.jake","password":"jakejake"}}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/profiles/Jacob"))
                .andExpect(content().json(USER_JACOB, STRICT));
    }

    @Test
    void deveResponder422ComAChaveDoCampoEmBranco() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"user":{"username":"","email":"jake@jake.jake","password":"jakejake"}}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"username":["can't be blank"]}}""", STRICT));
    }

    @Test
    void deveResponder422BodyParaJsonMalformado() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{\"user\":"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"body":["can't be empty"]}}""", STRICT));
    }

    @Test
    void deveResponder409QuandoServicoIndicaConflito() throws Exception {
        when(userService.cadastrar(anyString(), anyString(), anyString()))
                .thenThrow(new ConflictException("username"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"user":{"username":"Jacob","email":"jake@jake.jake","password":"jakejake"}}"""))
                .andExpect(status().isConflict())
                .andExpect(content().json("""
                        {"errors":{"username":["has already been taken"]}}""", STRICT));
    }

    @Test
    void deveResponder200NoLogin() throws Exception {
        when(userService.autenticar("jake@jake.jake", "jakejake")).thenReturn(JACOB);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"user":{"email":"jake@jake.jake","password":"jakejake"}}"""))
                .andExpect(status().isOk())
                .andExpect(content().json(USER_JACOB, STRICT));
    }

    @Test
    void deveResponder422NoLoginSemPassword() throws Exception {
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"user":{"email":"jake@jake.jake"}}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"password":["can't be blank"]}}""", STRICT));
    }

    @Test
    void deveResponder401IsMissingNoUsuarioAtualSemToken() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("""
                        {"errors":{"token":["is missing"]}}""", STRICT));
    }

    @Test
    void deveResponder200NoUsuarioAtualComJwt() throws Exception {
        when(userService.buscarAtual(7L)).thenReturn(JACOB);

        mockMvc.perform(get("/api/user").with(jwt().jwt(token -> token.subject("7"))))
                .andExpect(status().isOk())
                .andExpect(content().json(USER_JACOB, STRICT));
    }

    @Test
    void deveResponder422QuandoAtualizacaoSemCampos() throws Exception {
        when(userService.atualizar(eq(7L), any(UserUpdate.class)))
                .thenThrow(new ValidationException("user", "can't be empty"));

        mockMvc.perform(put("/api/user")
                        .with(jwt().jwt(token -> token.subject("7")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"user":{}}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"user":["can't be empty"]}}""", STRICT));
    }

    @Test
    void deveResponder422BodyQuandoAtualizacaoSemObjetoUser() throws Exception {
        mockMvc.perform(put("/api/user")
                        .with(jwt().jwt(token -> token.subject("7")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"body":["can't be empty"]}}""", STRICT));
    }
}
