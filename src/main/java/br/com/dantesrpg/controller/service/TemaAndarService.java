package br.com.dantesrpg.controller.service;

import java.io.InputStream;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import br.com.dantesrpg.model.theme.ConfiguracaoAndar;
import br.com.dantesrpg.model.theme.ConfiguracaoAndar.Assets;
import br.com.dantesrpg.model.theme.ConfiguracaoAndar.Paleta;
import br.com.dantesrpg.model.util.FileLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Region;

public class TemaAndarService {
	private static final String CLASSE_TEMA_ATIVO = "tema-andar-ativo";
	private static final String PREFIXO_CLASSE_TEMA = "tema-andar-";

	private final Region menuEsquerdo;
	private final Region barraInferior;
	private final Set<Parent> raizesRegistradas = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Map<Parent, String> estilosOriginais = new IdentityHashMap<>();
	private final Map<Region, Background> backgroundsOriginais = new IdentityHashMap<>();
	private final Set<Region> bannersAplicados = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Map<String, Image> cacheImagens = new java.util.HashMap<>();

	private ConfiguracaoAndar configuracaoAtual;

	public TemaAndarService(Parent raizPrincipal, Region menuEsquerdo, Region barraInferior) {
		this.menuEsquerdo = menuEsquerdo;
		this.barraInferior = barraInferior;
		registrarRaiz(raizPrincipal);
	}

	public void registrarRaiz(Parent raiz) {
		if (raiz == null || !raizesRegistradas.add(raiz)) {
			return;
		}
		estilosOriginais.put(raiz, raiz.getStyle());
		if (configuracaoAtual != null) {
			aplicarTokens(raiz, configuracaoAtual);
		}
	}

	public void aplicarTema(ConfiguracaoAndar configuracao) {
		this.configuracaoAtual = configuracao;
		for (Parent raiz : raizesRegistradas) {
			aplicarTokens(raiz, configuracao);
		}

		Assets assets = configuracao != null ? configuracao.getAssets() : null;
		aplicarBanner(menuEsquerdo, assets != null ? assets.getBannerMenuEsquerdo() : null);
		aplicarBanner(barraInferior, assets != null ? assets.getBannerBarraInferior() : null);
	}

	public Image carregarImagem(String caminho) {
		if (caminho == null || caminho.isBlank()) {
			return null;
		}
		Image imagemEmCache = cacheImagens.get(caminho);
		if (imagemEmCache != null) {
			return imagemEmCache;
		}
		Optional<Image> imagemCarregada = carregarImagemInterna(caminho);
		imagemCarregada.ifPresent(imagem -> cacheImagens.put(caminho, imagem));
		return imagemCarregada.orElse(null);
	}

	private void aplicarTokens(Parent raiz, ConfiguracaoAndar configuracao) {
		removerClassesDeTema(raiz);
		String estiloOriginal = estilosOriginais.getOrDefault(raiz, "");

		if (configuracao == null || configuracao.isNulo() || configuracao.getPaleta() == null) {
			raiz.setStyle(estiloOriginal);
			raiz.applyCss();
			return;
		}

		raiz.getStyleClass().add(CLASSE_TEMA_ATIVO);
		raiz.getStyleClass().add(configuracao.isTemaClaro() ? "tema-andar-claro" : "tema-andar-escuro");
		if (configuracao.getClasseCss() != null && !configuracao.getClasseCss().isBlank()) {
			raiz.getStyleClass().add(configuracao.getClasseCss());
		}

		Paleta p = configuracao.getPaleta();
		String tokens = String.join(" ",
				declarar("-andar-canvas", p.getCanvas()),
				declarar("-andar-superficie", p.getSuperficie()),
				declarar("-andar-superficie-elevada", p.getSuperficieElevada()),
				declarar("-andar-primaria", p.getPrimaria()),
				declarar("-andar-secundaria", p.getSecundaria()),
				declarar("-andar-acento", p.getAcento()),
				declarar("-andar-texto", p.getTexto()),
				declarar("-andar-texto-suave", p.getTextoSuave()),
				declarar("-andar-texto-sobre-primaria", p.getTextoSobrePrimaria()),
				declarar("-andar-borda", p.getBorda()),
				declarar("-andar-painel", p.getPainel()),
				declarar("-andar-card", p.getCard()),
				declarar("-andar-campo", p.getCampo()),
				declarar("-andar-hover", p.getHover()),
				declarar("-andar-pressionado", p.getPressionado()));
		raiz.setStyle((estiloOriginal == null ? "" : estiloOriginal) + " " + tokens);
		raiz.applyCss();
	}

	private void removerClassesDeTema(Parent raiz) {
		raiz.getStyleClass().removeIf(classe -> classe.equals(CLASSE_TEMA_ATIVO)
				|| classe.equals("tema-andar-claro") || classe.equals("tema-andar-escuro")
				|| (classe.startsWith(PREFIXO_CLASSE_TEMA) && !classe.equals(CLASSE_TEMA_ATIVO)));
	}

	private String declarar(String nome, String valor) {
		return valor == null || valor.isBlank() ? "" : nome + ": " + valor + ";";
	}

	private void aplicarBanner(Region regiao, String caminho) {
		if (regiao == null) {
			return;
		}
		Image imagem = carregarImagem(caminho);
		if (imagem == null) {
			if (bannersAplicados.remove(regiao)) {
				regiao.setBackground(backgroundsOriginais.get(regiao));
			}
			return;
		}

		regiao.applyCss();
		backgroundsOriginais.putIfAbsent(regiao, regiao.getBackground());
		BackgroundImage banner = new BackgroundImage(imagem,
				BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
				BackgroundPosition.CENTER,
				new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO,
						false, false, false, true));
		regiao.setBackground(new Background(banner));
		bannersAplicados.add(regiao);
	}

	private Optional<Image> carregarImagemInterna(String caminho) {
		try (InputStream input = FileLoader.carregarArquivoOpcional(caminho)) {
			if (input == null) {
				return Optional.empty();
			}
			Image imagem = new Image(input);
			return imagem.isError() ? Optional.empty() : Optional.of(imagem);
		} catch (Exception e) {
			System.err.println("TEMA: Não foi possível carregar o asset " + caminho + ": " + e.getMessage());
			return Optional.empty();
		}
	}
}
