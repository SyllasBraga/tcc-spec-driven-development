package com.example.blog.shared.seguranca;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.blog.ClienteApiDeTeste;
import com.example.blog.ClienteApiDeTeste.Conta;
import com.example.blog.TestcontainersConfiguration;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AutenticacaoIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private JwtProperties propriedades;

	@Test
	void semCabecalhoDeveResponderTokenAusente() throws Exception {
		assertToken401(mockMvc.perform(get("/api/user")), "is missing");
	}

	@Test
	void prefixoBearerDeveSerRecusado() throws Exception {
		Conta conta = new ClienteApiDeTeste(mockMvc, jsonMapper).cadastrar("bearer");

		assertToken401(mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Bearer " + conta.token())),
				"is invalid");
	}

	@Test
	void tokenMalformadoDeveSerRecusado() throws Exception {
		assertToken401(comToken("nao-e-um-jwt"), "is invalid");
	}

	@Test
	void tokenAssinadoComOutraChaveDeveSerRecusado() throws Exception {
		byte[] outraChave = Base64.getDecoder().decode(SegredoJwtDeTesteConfiguration.gerarSegredo());
		JwtEncoder outroEncoder =
				new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(outraChave, "HmacSHA256")));
		Instant agora = Instant.now();

		assertToken401(comToken(emitir(outroEncoder, propriedades.issuer(), agora, agora.plus(Duration.ofHours(1)))),
				"is invalid");
	}

	@Test
	void tokenExpiradoDeveSerRecusado() throws Exception {
		Instant emitidoEm = Instant.now().minus(Duration.ofDays(2));

		assertToken401(comToken(emitir(jwtEncoder, propriedades.issuer(), emitidoEm, emitidoEm.plus(Duration.ofDays(1)))),
				"is invalid");
	}

	@Test
	void tokenDeOutroEmissorDeveSerRecusado() throws Exception {
		Instant agora = Instant.now();

		assertToken401(comToken(emitir(jwtEncoder, "outro-emissor", agora, agora.plus(Duration.ofHours(1)))),
				"is invalid");
	}

	@Test
	void tokenDeUsuarioInexistenteDeveSerRecusado() throws Exception {
		Instant agora = Instant.now();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
				JwtClaimsSet.builder()
					.subject(String.valueOf(Long.MAX_VALUE))
					.issuer(propriedades.issuer())
					.issuedAt(agora)
					.expiresAt(agora.plus(Duration.ofHours(1)))
					.build()))
			.getTokenValue();

		assertToken401(comToken(token), "is invalid");
	}

	@Test
	void atualizarSemTokenDeveResponderTokenAusente() throws Exception {
		assertToken401(mockMvc.perform(put("/api/user").contentType(MediaType.APPLICATION_JSON)
			.content("{\"user\":{\"bio\":\"x\"}}")), "is missing");
	}

	private ResultActions comToken(String token) throws Exception {
		return mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Token " + token));
	}

	private static String emitir(JwtEncoder encoder, String emissor, Instant emitidoEm, Instant expiraEm) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject("1")
			.issuer(emissor)
			.issuedAt(emitidoEm)
			.expiresAt(expiraEm)
			.build();
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}

	private static void assertToken401(ResultActions resultado, String mensagem) throws Exception {
		resultado.andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("charset=UTF-8")))
			.andExpect(jsonPath("$.errors.token[0]").value(mensagem));
	}

}
