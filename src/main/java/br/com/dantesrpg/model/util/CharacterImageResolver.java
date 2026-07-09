package br.com.dantesrpg.model.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import br.com.dantesrpg.model.Personagem;
import javafx.scene.image.Image;

public final class CharacterImageResolver {

	private static final List<String> EXTENSOES_SUPORTADAS = List.of(".png", ".jpg", ".jpeg", ".webp");

	private CharacterImageResolver() {
	}

	public static Image getPortrait(Personagem personagem, double largura, double altura) {
		return carregarPrimeiraImagem(resolverCaminhosPortrait(personagem), largura, altura);
	}

	public static Image getToken(Personagem personagem, double largura, double altura) {
		return carregarPrimeiraImagem(resolverCaminhosToken(personagem), largura, altura);
	}

	public static Image getTokenPorNome(String nome, double largura, double altura) {
		List<String> nomes = new ArrayList<>();
		nomes.add(nome);
		return carregarPrimeiraImagem(resolverCaminhosPorNomes("/tokens/", nomes), largura, altura);
	}

	public static Image getTokenPorNomes(List<String> nomes, double largura, double altura) {
		return carregarPrimeiraImagem(resolverCaminhosPorNomes("/tokens/", nomes), largura, altura);
	}

	public static List<String> resolverCaminhosPortrait(Personagem personagem) {
		return resolverCaminhosPorNomes("/portraits/", nomesCandidatos(personagem));
	}

	public static List<String> resolverCaminhosToken(Personagem personagem) {
		return resolverCaminhosPorNomes("/tokens/", nomesCandidatos(personagem));
	}

	private static Image carregarPrimeiraImagem(List<String> caminhos, double largura, double altura) {
		for (String caminho : caminhos) {
			Image imagem = ImageCache.get(caminho, largura, altura);
			if (imagem != null && !imagem.isError()) {
				return imagem;
			}
		}
		return null;
	}

	private static List<String> resolverCaminhosPorNomes(String pasta, List<String> nomes) {
		Set<String> caminhos = new LinkedHashSet<>();
		List<String> arquivos = FileLoader.listarArquivosDeDiretorio(pasta, null);

		for (String nome : nomes) {
			String base = normalizarNomeArquivo(nome);
			if (base.isBlank()) {
				continue;
			}
			for (String arquivo : arquivos) {
				if (!extensaoSuportada(arquivo)) {
					continue;
				}
				if (removerExtensao(arquivo).equalsIgnoreCase(base)) {
					caminhos.add(pasta + arquivo);
				}
			}
			caminhos.add(pasta + base + ".png");
		}

		return new ArrayList<>(caminhos);
	}

	private static List<String> nomesCandidatos(Personagem personagem) {
		if (personagem == null) {
			return List.of();
		}

		Personagem referencia = personagem;
		if (personagem.isClone() && personagem.getCriador() != null) {
			referencia = personagem.getCriador();
		}

		List<String> candidatos = new ArrayList<>();
		adicionarCandidato(candidatos, referencia.getNome());
		adicionarCandidato(candidatos, limparNomeDeSpawn(referencia.getNome()));
		adicionarCandidato(candidatos, referencia.getNomeBaseImagem());
		adicionarCandidato(candidatos, limparNomeDeSpawn(referencia.getNomeBaseImagem()));
		adicionarCandidato(candidatos, referencia.getJsonFileName());
		adicionarCandidato(candidatos, removerExtensao(referencia.getJsonFileName()));
		return candidatos;
	}

	private static void adicionarCandidato(List<String> candidatos, String valor) {
		if (valor == null || valor.isBlank()) {
			return;
		}
		String limpo = valor.trim();
		if (!candidatos.contains(limpo)) {
			candidatos.add(limpo);
		}
	}

	public static String limparNomeDeSpawn(String nome) {
		if (nome == null) {
			return "";
		}
		return nome.toLowerCase(Locale.ROOT)
				.replaceFirst("^servo:\\s*", "")
				.replaceAll("\\s*\\d+$", "")
				.trim();
	}

	private static String normalizarNomeArquivo(String valor) {
		if (valor == null) {
			return "";
		}
		return removerExtensao(valor)
				.trim()
				.toLowerCase(Locale.ROOT)
				.replaceFirst("^servo:\\s*", "")
				.replaceAll("\\s*\\d+$", "")
				.replaceAll("\\s+", "_");
	}

	private static String removerExtensao(String arquivo) {
		if (arquivo == null) {
			return "";
		}
		int ponto = arquivo.lastIndexOf('.');
		if (ponto <= 0) {
			return arquivo;
		}
		return arquivo.substring(0, ponto);
	}

	private static boolean extensaoSuportada(String arquivo) {
		String extensao = obterExtensao(arquivo);
		return EXTENSOES_SUPORTADAS.contains(extensao);
	}

	private static String obterExtensao(String arquivo) {
		if (arquivo == null) {
			return "";
		}
		int ponto = arquivo.lastIndexOf('.');
		if (ponto <= 0) {
			return "";
		}
		return arquivo.substring(ponto).toLowerCase(Locale.ROOT);
	}
}
