package br.com.dantesrpg.model.map;

import br.com.dantesrpg.model.Personagem;

public class TerrainData {

	public enum TipoTerreno {
		PADRAO, PAREDE, CARVAO, OBJETO, SAIDA, LAVA
	}

	public enum TipoEfeitoSolo {
		FOGO, ACIDO, GAS, ELETRICIDADE, PORTAL
	}

	public static class EfeitoInstance {
		private TipoEfeitoSolo tipo;
		private int duracaoTU;
		private int danoPorTick; // Dano base (ex: 10% da arma do boss)
		private Personagem criador;
		private boolean permanente;

		public EfeitoInstance(TipoEfeitoSolo tipo, int duracaoTU, int danoPorTick, Personagem criador) {
			this.tipo = tipo;
			this.duracaoTU = duracaoTU;
			this.danoPorTick = danoPorTick;
			this.criador = criador;
			this.permanente = false;
		}

		public void reduzirDuracao(int custo) {
			if (!permanente) {
				this.duracaoTU -= custo;
			}
		}

		public boolean expirou() {
			return !permanente && duracaoTU <= 0;
		}

		public TipoEfeitoSolo getTipo() {
			return tipo;
		}

		public int getDanoPorTick() {
			return danoPorTick;
		}

		public Personagem getCriador() {
			return criador;
		}

		public void setPermanente(boolean p) {
			this.permanente = p;
		}

		public boolean isPermanente() {
			return permanente;
		}
	}
}