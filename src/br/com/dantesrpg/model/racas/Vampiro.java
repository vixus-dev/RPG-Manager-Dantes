package br.com.dantesrpg.model.racas;

import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.enums.TipoEfeito;
import java.util.List;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Raça;
import java.util.ArrayList;
import br.com.dantesrpg.model.habilidades.raciais.FormaDeMorcego;

public class Vampiro extends Raça {

	private final String nome = "Vampiro";
	private final String descricaoPassiva = "Sede de Sangue: Cura 25% do dano causado a inimigos que estejam sangrando.";

	@Override
	public String getNome() {
		return nome;
	}

	@Override
	public String getDescricaoPassiva() {
		return descricaoPassiva;
	}

	public void toggleTransform(Personagem personagem) {
		this.isTransformed = !this.isTransformed; // Inverte o estado

		String nomeEfeito = "Forma de Cobra"; // O nome do Efeito

		if (this.isTransformed) {
			// Se ENTROU na forma
			System.out.println(">>> " + personagem.getNome() + " entra na " + nomeEfeito + "!");

			Efeito efeitoForma = new Efeito(nomeEfeito, TipoEfeito.BUFF, 99999, null, 0, 0);
			personagem.adicionarEfeito(efeitoForma);

		} else {
			System.out.println(">>> " + personagem.getNome() + " sai da " + nomeEfeito + "!");
			personagem.removerEfeito(nomeEfeito);
		}

		personagem.recalcularAtributosEstatisticas();
	}

	@Override
	public double getBonusDanoPercentual(Personagem personagem) {
		if (this.isTransformed) {
			return -0.40; // Retorna -40% de dano
		}

		return super.getBonusDanoPercentual(personagem);
	}

	@Override
	public List<Habilidade> getRacialAbilities(Personagem personagem) {
		List<Habilidade> abilities = new ArrayList<>();
		abilities.add(new FormaDeMorcego());
		return abilities;
	}

	@Override
	public void onDamageDealt(Personagem personagem, Personagem alvo, double danoCausado, EstadoCombate estado,
			br.com.dantesrpg.controller.CombatController controller) {

		if (alvo != null && danoCausado > 0 && alvo.getEfeitosAtivos().containsKey("Sangramento")) {
			int cura = (int) (danoCausado * 0.25);
			if (cura > 0) {
				personagem.setVidaAtual(personagem.getVidaAtual() + cura, estado, controller);
				System.out
						.println(">>> Sede de Sangue (Vampiro) curou " + personagem.getNome() + " em " + cura + " HP!");
			}
		}

		if (!this.isTransformed || alvo == null || danoCausado <= 0) {
			return;
		}

		System.out.println(">>> Forma de Cobra: Verificando chances de efeito...");

		boolean aplicaSangramento = Math.random() < 0.50;
		boolean aplicaVeneno = Math.random() < 0.50;

		// --- DEFINIÇÕES DOS EFEITOS (Baseado no danoCausado) ---

		int intervaloTick = 67;
		int intervaloTickToxina = 50;

		// Sangramento: 5 ticks, 25% do 'danoCausado' por tick
		int dano_sangramento = Math.max(1, (int) (danoCausado * 0.25));
		int duracao_sangramento = 5 * intervaloTick; // 325 TU

		// Veneno: 6 ticks, 20% do 'danoCausado' por tick
		int dano_veneno = Math.max(1, (int) (danoCausado * 0.20));
		int duracao_veneno = 6 * intervaloTick; // 402 TU

		// Toxina: 4 ticks, 50% do 'danoCausado' por tick
		int dano_toxina = Math.max(1, (int) (danoCausado * 0.50));
		int duracao_toxina = 4 * intervaloTickToxina; // 200 TU

		boolean efeitoAplicado = false;

		if (aplicaSangramento && aplicaVeneno) {
			// Causa Toxina (em vez dos outros dois)
			System.out.println(">>> SUCESSO! Aplica [Toxina]!");
			Efeito toxina = new Efeito("Toxina", TipoEfeito.DOT, duracao_toxina, null, dano_toxina, intervaloTick);
			alvo.adicionarEfeito(toxina);
			efeitoAplicado = true;

		} else if (aplicaSangramento) {
			// Causa Sangramento
			System.out.println(">>> SUCESSO! Aplica [Sangramento]!");
			Efeito sangramento = new Efeito("Sangramento", TipoEfeito.DOT, duracao_sangramento, null, dano_sangramento,
					intervaloTick);
			alvo.adicionarEfeito(sangramento);
			efeitoAplicado = true;

		} else if (aplicaVeneno) {
			// Causa Veneno
			System.out.println(">>> SUCESSO! Aplica [Veneno]!");
			Efeito veneno = new Efeito("Veneno", TipoEfeito.DOT, duracao_veneno, null, dano_veneno, intervaloTick);
			alvo.adicionarEfeito(veneno);
			efeitoAplicado = true;
		}

		// Recalcula os stats do alvo se um efeito foi aplicado (para a UI mostrar o debuff)
		if (efeitoAplicado) {
			alvo.recalcularAtributosEstatisticas();
		}
	}
}