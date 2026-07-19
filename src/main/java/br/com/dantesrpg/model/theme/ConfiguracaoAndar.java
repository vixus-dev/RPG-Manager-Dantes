package br.com.dantesrpg.model.theme;

import br.com.dantesrpg.model.EstadoAndarParty;
import br.com.dantesrpg.model.enums.AndarCampanha;

public class ConfiguracaoAndar {
	private String id;
	private String andarId;
	private int estadoVisual;
	private String nome;
	private String opcaoSeletor;
	private String classeCss;
	private boolean temaClaro;
	private Paleta paleta;
	private Assets assets;

	public String getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getOpcaoSeletor() {
		return opcaoSeletor;
	}

	public String getClasseCss() {
		return classeCss;
	}

	public boolean isTemaClaro() {
		return temaClaro;
	}

	public Paleta getPaleta() {
		return paleta;
	}

	public Assets getAssets() {
		return assets;
	}

	public EstadoAndarParty getEstado() {
		return new EstadoAndarParty(AndarCampanha.fromId(andarId), estadoVisual);
	}

	public boolean isNulo() {
		return getEstado().andar() == AndarCampanha.NULO;
	}

	public static ConfiguracaoAndar criarNulo() {
		ConfiguracaoAndar config = new ConfiguracaoAndar();
		config.id = "NULO";
		config.andarId = AndarCampanha.NULO.name();
		config.estadoVisual = 0;
		config.nome = "Tema atual";
		config.opcaoSeletor = "Nenhum";
		config.classeCss = "";
		return config;
	}

	public static class Paleta {
		private String canvas;
		private String superficie;
		private String superficieElevada;
		private String primaria;
		private String secundaria;
		private String acento;
		private String texto;
		private String textoSuave;
		private String textoSobrePrimaria;
		private String borda;
		private String painel;
		private String card;
		private String campo;
		private String hover;
		private String pressionado;

		public String getCanvas() { return canvas; }
		public String getSuperficie() { return superficie; }
		public String getSuperficieElevada() { return superficieElevada; }
		public String getPrimaria() { return primaria; }
		public String getSecundaria() { return secundaria; }
		public String getAcento() { return acento; }
		public String getTexto() { return texto; }
		public String getTextoSuave() { return textoSuave; }
		public String getTextoSobrePrimaria() { return textoSobrePrimaria; }
		public String getBorda() { return borda; }
		public String getPainel() { return painel; }
		public String getCard() { return card; }
		public String getCampo() { return campo; }
		public String getHover() { return hover; }
		public String getPressionado() { return pressionado; }
	}

	public static class Assets {
		private String bannerMenuEsquerdo;
		private String bannerBarraInferior;
		private String fundoLoja;
		private String fundoEditor;

		public String getBannerMenuEsquerdo() { return bannerMenuEsquerdo; }
		public String getBannerBarraInferior() { return bannerBarraInferior; }
		public String getFundoLoja() { return fundoLoja; }
		public String getFundoEditor() { return fundoEditor; }
	}
}
