package br.com.dantesrpg.model.armas.unicas;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.ArmaRanged;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.enums.TipoEfeito;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Terrore extends ArmaRanged {

	private Map<Personagem, Integer> marcasDoMedo;

	public Terrore() {
		// Dano 4, Ticks 1, Alcance 7, Munição 7
		super("Terrore", "Pistolas",
				"Terrore é uma pistola estilo revólver, não se sabe do que o revólver é feito, mas não parece ser de ferro, você consegue confrontar seus medos ?",
				Raridade.UNICO, 0, 4, 1, Atributo.SAGACIDADE, 80, 4, 7);
		this.marcasDoMedo = new HashMap<>();
	}

	@Override
	public void onCombatStart(Personagem ator, EstadoCombate estado) {
		this.marcasDoMedo.clear();

		// Encontra todos os inimigos vivos
		List<Personagem> inimigos = estado.getCombatentes().stream()
				.filter(p -> p.isAtivoNoCombate() && !p.getFaccao().equals(ator.getFaccao()))
				.collect(Collectors.toList());

		// Ordena pela Vida Máxima (descendente)
		inimigos.sort(Comparator.comparingDouble(Personagem::getVidaMaxima).reversed());

		System.out.println(">>> TERRORE: Marcando alvos 'O Medo Sabe seu Nome'...");

		// Pega os Top 7 e aplica o ID (7 para o mais alto, 1 para o 7º)
		int idMarca = 7;
		for (int i = 0; i < inimigos.size() && i < 7; i++) {
			Personagem inimigo = inimigos.get(i);
			this.marcasDoMedo.put(inimigo, idMarca);
			System.out.println(">>> MARCA APLICADA: " + inimigo.getNome() + " (HP: " + inimigo.getVidaMaxima()
					+ ") recebe ID = " + idMarca);
			idMarca--;
		}
	}

	@Override
	public double getBonusDanoArma(Personagem ator, Personagem alvo, EstadoCombate estado, AcaoMestreInput input) {
		double bonus = 1.0;

		if (this.marcasDoMedo.containsKey(alvo)) {
			int idDoAlvo = this.marcasDoMedo.get(alvo);
			int rolagemD7 = input.getResultadoDado("DADO_MEDO_D7");

			if (rolagemD7 == idDoAlvo) {
				System.out.println(
						">>> TERRORE (ID " + idDoAlvo + "): Acerto Crítico de Medo! (+20% Dano, Dilaceramento)");
				bonus += 0.20;

				Efeito dilaceramento = new Efeito("Dilaceramento", TipoEfeito.DEBUFF, 300, Map.of(), 0, 0);
				alvo.adicionarEfeito(dilaceramento);
				alvo.recalcularAtributosEstatisticas();
			}
		}

		if (alvo.getRaca() instanceof br.com.dantesrpg.model.racas.Vampiro) {
			int balasFaltando = this.getMunicaoMaxima() - this.getMunicaoAtual();
			double bonusVampiro = balasFaltando * 0.05;

			if (bonusVampiro > 0) {
				System.out.println(">>> TERRORE (Prata): +" + (int) (bonusVampiro * 100) + "% contra Vampiro ("
						+ balasFaltando + " balas vazias).");
				bonus += bonusVampiro;
			}
		}

		return bonus;
	}
}