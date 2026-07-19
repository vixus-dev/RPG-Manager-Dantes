package br.com.dantesrpg.model.theme;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import br.com.dantesrpg.model.EstadoAndarParty;
import br.com.dantesrpg.model.util.FileLoader;

public class CatalogoTemasAndar {
	private static final String CAMINHO_CONFIGURACAO = "/data/andares.json";

	private final Map<String, ConfiguracaoAndar> configuracoesPorId = new LinkedHashMap<>();
	private final Map<String, ConfiguracaoAndar> configuracoesPorOpcao = new LinkedHashMap<>();
	private final ConfiguracaoAndar configuracaoNula;

	public CatalogoTemasAndar() {
		List<ConfiguracaoAndar> configuracoes = carregarConfiguracoes();
		for (ConfiguracaoAndar configuracao : configuracoes) {
			if (configuracao == null || configuracao.getId() == null) {
				continue;
			}
			configuracoesPorId.put(normalizar(configuracao.getId()), configuracao);
			if (configuracao.getOpcaoSeletor() != null) {
				configuracoesPorOpcao.put(normalizar(configuracao.getOpcaoSeletor()), configuracao);
			}
		}
		this.configuracaoNula = configuracoesPorId.getOrDefault("nulo", ConfiguracaoAndar.criarNulo());
	}

	public ConfiguracaoAndar buscarPorEstado(EstadoAndarParty estado) {
		if (estado == null) {
			return configuracaoNula;
		}
		return configuracoesPorId.getOrDefault(normalizar(estado.getChave()), configuracaoNula);
	}

	public ConfiguracaoAndar buscarPorOpcao(String opcao) {
		if (opcao == null) {
			return configuracaoNula;
		}
		return configuracoesPorOpcao.getOrDefault(normalizar(opcao), configuracaoNula);
	}

	public ConfiguracaoAndar getConfiguracaoNula() {
		return configuracaoNula;
	}

	public List<String> getOpcoesSeletor() {
		return configuracoesPorId.values().stream()
				.map(ConfiguracaoAndar::getOpcaoSeletor)
				.filter(opcao -> opcao != null && !opcao.isBlank())
				.toList();
	}

	private List<ConfiguracaoAndar> carregarConfiguracoes() {
		try (InputStream input = FileLoader.carregarArquivo(CAMINHO_CONFIGURACAO)) {
			if (input == null) {
				System.err.println("TEMA: Configuração de andares não encontrada. Usando tema atual.");
				return List.of(ConfiguracaoAndar.criarNulo());
			}
			try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
				List<ConfiguracaoAndar> configuracoes = new Gson().fromJson(reader,
						new TypeToken<List<ConfiguracaoAndar>>() { }.getType());
				return configuracoes != null ? configuracoes : new ArrayList<>();
			}
		} catch (Exception e) {
			System.err.println("TEMA: Falha ao carregar andares.json: " + e.getMessage());
			return List.of(ConfiguracaoAndar.criarNulo());
		}
	}

	private String normalizar(String valor) {
		return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
	}
}
