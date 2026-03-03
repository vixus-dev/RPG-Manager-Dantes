package br.com.dantesrpg.model.map;

import java.util.ArrayList;
import java.util.List;

public class MapMetadata {
	private List<Ponto> paredes = new ArrayList<>();
	private List<Ponto> saidas = new ArrayList<>();
	private List<ObjetoData> objetos = new ArrayList<>();
	private List<InimigoSpawn> inimigos = new ArrayList<>();

	public List<Ponto> getParedes() {
		return paredes;
	}

	public List<Ponto> getSaidas() {
		return saidas;
	}

	public List<ObjetoData> getObjetos() {
		return objetos;
	}

	public List<InimigoSpawn> getInimigos() {
		return inimigos;
	}

	// --- Classes Internas Auxiliares ---

	public static class Ponto {
		public int x, y;

		public Ponto(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public static class ObjetoData {
		public int x, y;
		public int hp;

		public ObjetoData(int x, int y, int hp) {
			this.x = x;
			this.y = y;
			this.hp = hp;
		}
	}

	public static class InimigoSpawn {
		public int x, y;
		public String idMonstro; // Ex: "DemonioMenor"

		public InimigoSpawn(int x, int y, String id) {
			this.x = x;
			this.y = y;
			this.idMonstro = id;
		}
	}
}