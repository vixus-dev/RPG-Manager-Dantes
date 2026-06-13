package br.com.dantesrpg.model.map;

import java.util.Random;

public class MapGenerator {

	public static TileDefinition[][] gerarMapaProcedural(int largura, int altura, String bioma, String densidadeStr) {
		TileDefinition[][] mapa = new TileDefinition[largura][altura];
		Random rand = new Random();
		TileRegistry registry = TileRegistry.getInstance();

		String idChaoBase = "floor";
		String idChaoSecundario = "floor";
		String idParede = "wall";
		String idObstaculo = "object";
		String idEfeito = null;

		switch (bioma) {
		case "Laboratório":
			idChaoBase = "floor9";
			idChaoSecundario = "floor";
			idParede = "wall7";
			idEfeito = "acido";
			break;
		case "Caverna Grega":
			idChaoBase = "floor4";
			idChaoSecundario = "SandBrick";
			idParede = "wall4";
			idEfeito = "sand";
			break;
		case "Inferno":
			idChaoBase = "floor8";
			idChaoSecundario = "floor2";
			idParede = "wall8";
			idEfeito = "lava";
			break;
		case "Planície":
			idChaoBase = "grass1";
			idChaoSecundario = "floor";
			idParede = "wall";
			idEfeito = "sand";
			break;
		case "Salão Escuro":
			idChaoBase = "floor2";
			idChaoSecundario = "floor";
			idParede = "wall2";
			idEfeito = "sangue";
			break;
		default:
			break;
		}

		double densidade = 0.15;
		if ("Baixa".equals(densidadeStr)) {
			densidade = 0.05;
		} else if ("Alta".equals(densidadeStr)) {
			densidade = 0.30;
		}

		TileDefinition chaoBase = registry.getById(idChaoBase);
		TileDefinition chaoSec = registry.getById(idChaoSecundario);
		TileDefinition parede = registry.getById(idParede);
		TileDefinition obs = registry.getById(idObstaculo);
		TileDefinition efeito = idEfeito != null ? registry.getById(idEfeito) : null;

		if (chaoBase == null) chaoBase = registry.getDefault();
		if (chaoSec == null) chaoSec = chaoBase;
		if (parede == null) parede = registry.getDefault();
		if (obs == null) obs = parede;

		for (int x = 0; x < largura; x++) {
			for (int y = 0; y < altura; y++) {
				// Paredes da borda
				if (x == 0 || y == 0 || x == largura - 1 || y == altura - 1) {
					mapa[x][y] = parede;
				} else {
					// Preenchimento interno
					double r = rand.nextDouble();
					if (r < densidade) {
						mapa[x][y] = (rand.nextBoolean() ? parede : obs);
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
