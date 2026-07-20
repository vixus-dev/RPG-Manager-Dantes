package br.com.dantesrpg.model.map;

import java.util.Random;

public class MapGenerator {

	public static TileDefinition[][] gerarMapaProcedural(int largura, int altura, String bioma, String densidadeStr) {
		TileDefinition[][] mapa = new TileDefinition[largura][altura];
		Random rand = new Random();
		TileRegistry registry = TileRegistry.getInstance();

		if ("Ira Externo".equalsIgnoreCase(bioma)) {
			TileDefinition tileAreia = registry.getById("areia");
			TileDefinition tileAgua = registry.getById("agua");
			if (tileAreia == null) tileAreia = registry.getDefault();
			if (tileAgua == null) tileAgua = registry.getDefault();
			boolean dividirVertical = rand.nextBoolean();
			double percentualCorte = 0.45 + rand.nextDouble() * 0.10; // 45% a 55%
			boolean areiaPrimeiraMetade = rand.nextBoolean();
			int limiteCorte = (int) (dividirVertical ? (largura * percentualCorte) : (altura * percentualCorte));
			for (int x = 0; x < largura; x++) {
				for (int y = 0; y < altura; y++) {
					if (x == 0 || y == 0 || x == largura - 1 || y == altura - 1) {
						TileDefinition paredeStone = registry.getById("sandStone");
						mapa[x][y] = (paredeStone != null) ? paredeStone : registry.getDefault();
					} else {
						boolean isPrimeiraMetade = dividirVertical ? (x < limiteCorte) : (y < limiteCorte);
						if (isPrimeiraMetade) {
							mapa[x][y] = areiaPrimeiraMetade ? tileAreia : tileAgua;
						} else {
							mapa[x][y] = areiaPrimeiraMetade ? tileAgua : tileAreia;
						}
					}
				}
			}
			// Limpa o centro do mapa para spawn seguro dos personagens
			int meioX = largura / 2;
			int meioY = altura / 2;
			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					if (meioX + i > 0 && meioX + i < largura - 1 && meioY + j > 0 && meioY + j < altura - 1) {
						mapa[meioX + i][meioY + j] = tileAreia;
					}
				}
			}
			return mapa;
		}

		String idChaoBase = null;
		String idChaoSecundario = null;
		String idParede = null;
		String idObstaculo = null;
		String idEfeito = null;

		switch (bioma) {
		case "Superficie":
			idChaoBase = "floor";
			idParede = "wall";
			break;
		case "E.D.E.N":
			idChaoBase = "floor";
			idParede = "wall";
			break;
		case "E.D.E.N Segundo":
			idChaoBase = "floor3";
			idParede = "wall3";
			break;
		case "E.D.E.N Externo":
			idChaoBase = "floor2";
			idParede = "wall2";
			idEfeito = "coal";
			break;
		case "Limbo":
			idChaoBase = "grass1";
			break;
		case "Templo Grego":
			idChaoBase = "floor4";
			idParede = "wall4";
			break;
		case "Inferno":
			idChaoBase = "floor5";
			idEfeito = "lava";
			break;
		case "Clair De Lune":
			idChaoBase = "floor6";
			idParede = "wall5";
			break;
		case "Luxuria Interno":
			idChaoBase = "floor7";
			idParede = "wall7";
			break;
		case "Luxuria Externo":
			idChaoBase = "floor7";
			idParede = "wall6";
			break;
		case "Gula":
			idChaoBase = "floor8";
			idParede = "wall8";
			idEfeito = "sangue";
			break;
		case "Gula Factory":
			idChaoBase = "floor9";
			idParede = "wall7";
			idEfeito = "acido";
			break;
		case "Ganacia Externo":
			idChaoBase = "sand";
			idChaoSecundario = "SandBrick";
			idEfeito = "LiquidGold";
			break;
		case "Ganacia Interno":
			idChaoBase = "SandBrick";
			idParede = "sandStone";
			break;
		case "Ira Interno":
			idChaoBase = "floor6";
			idParede = "wall7";
			break;
		case "Ira Externo":
			idChaoBase = "areia";
			idEfeito = "agua";
			break;
		case "Ira Submerso Externo":
			idChaoBase = "aguaProfunda";
			break;
		case "Ira Externo FreeRoam":
			idChaoBase = "areia";
			idChaoSecundario = "sandStone";
			break;
		case "Exemple":
			idChaoBase = "floor7";
			idChaoSecundario = "floor";
			idParede = "wall7";
			idEfeito = "acido";
			break;
		default:
			break;
		}

		double densidade = 0.10;
		
		if ("Baixa".equals(densidadeStr)) {
			densidade = 0.05;
		} else if ("Alta".equals(densidadeStr)) {
			densidade = 0.30;
		}

		TileDefinition chaoBase = idChaoBase != null ? registry.getById(idChaoBase) : null;
		TileDefinition chaoSec = idChaoSecundario != null ? registry.getById(idChaoSecundario) : null;
		TileDefinition parede = idParede != null ? registry.getById(idParede) : null;
		TileDefinition obs = idObstaculo != null ? registry.getById(idObstaculo) : null;
		TileDefinition efeito = idEfeito != null ? registry.getById(idEfeito) : null;

		if (chaoBase == null) chaoBase = registry.getDefault();
		if (chaoSec == null) chaoSec = chaoBase;

		for (int x = 0; x < largura; x++) {
			for (int y = 0; y < altura; y++) {
				// Paredes da borda
				if (x == 0 || y == 0 || x == largura - 1 || y == altura - 1) {
					mapa[x][y] = (parede != null) ? parede : chaoBase;
				} else {
					// Preenchimento interno
					double r = rand.nextDouble();
					if (r < densidade) {
						if (parede != null && obs != null) {
							mapa[x][y] = (rand.nextBoolean() ? parede : obs);
						} else if (parede != null) {
							mapa[x][y] = parede;
						} else if (obs != null) {
							mapa[x][y] = obs;
						} else {
							mapa[x][y] = chaoBase;
						}
					} else if (r < densidade + 0.10 && efeito != null) {
						mapa[x][y] = efeito;
					} else if (r < densidade + 0.30) {
						mapa[x][y] = chaoSec;
					} else {
						mapa[x][y] = chaoBase;
					}
				}
			}
		}

		// Limpa o centro do mapa para spawn seguro dos personagens
		int meioX = largura / 2;
		int meioY = altura / 2;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (meioX + i > 0 && meioX + i < largura - 1 && meioY + j > 0 && meioY + j < altura - 1) {
					mapa[meioX + i][meioY + j] = chaoBase;
				}
			}
		}

		return mapa;
	}
}
