package br.com.dantesrpg.model.racas;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.enums.TipoAcao;
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
		if (isV2) {
			return "Bad Omen: Sede de Sangue + Forma de Cobra cria aura 5x5 (10% dano arma a cada 50TU, cura 100% do dano da aura).";
		}
		return descricaoPassiva;
	}

	@Override
	public String getNomeV2() {
		return "Bad Omen";
	}

	public void toggleTransform(Personagem personagem) {
		this.isTransformed = !this.isTransformed; // Inverte o estado

		String nomeEfeito = "Forma de Cobra";

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
		FormaDeMorcego forma = new FormaDeMorcego();
		if (isV2) {
			forma.setDescricao("Alterna Forma de Cobra (Bad Omen): cria aura 5x5 que causa 10% dano da arma a cada 50TU e cura 100% do dano da aura.");
		}
		abilities.add(forma);
		return abilities;
	}

	@Override
	public void onTimeAdvanced(Personagem personagem, EstadoCombate estado, CombatController controller) {
		// V2 (Bad Omen): Aura de dano 5x5 enquanto transformado
		if (!isV2 || !this.isTransformed || estado == null || controller == null)
			return;

		int tempoGlobal = estado.getTickCounter();
		if (tempoGlobal % 50 != 0 || tempoGlobal == 0)
			return;

		if (personagem.getArmaEquipada() == null || !personagem.isAtivoNoCombate())
			return;

		int danoAura = Math.max(1, (int) (personagem.getArmaEquipada().getDanoBase() * 0.10));
		int raio = 2; // 5x5 = raio 2 (centro + 2 em cada direção)
		int cx = personagem.getPosX();
		int cy = personagem.getPosY();
		String faccao = personagem.getFaccao();
		double curaTotal = 0;

		for (Personagem alvo : estado.getCombatentes()) {
			if (!alvo.isAtivoNoCombate() || alvo == personagem)
				continue;
			if (alvo.getFaccao().equals(faccao))
				continue;

			int dist = Math.max(Math.abs(alvo.getPosX() - cx), Math.abs(alvo.getPosY() - cy));
			if (dist <= raio) {
				CombatManager manager = controller.getCombatManager();
				manager.aplicarDanoAoAlvo(personagem, alvo, danoAura, false, TipoAcao.REACAO_FANTASMA, estado);
				curaTotal += danoAura;
				System.out.println(
						">>> BAD OMEN: Aura causa " + danoAura + " de dano em " + alvo.getNome() + "!");
			}
		}

		if (curaTotal > 0) {
			personagem.setVidaAtual(personagem.getVidaAtual() + curaTotal, estado, controller);
			System.out.println(">>> BAD OMEN: Aura curou " + personagem.getNome() + " em " + (int) curaTotal + " HP!");
		}
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

		// --- EFEITOS VIA FACTORY (Baseado no danoCausado) ---
		// A Factory calcula o dano por tick como % do dano real do golpe
		int danoDaSource = Math.max(1, (int) danoCausado);
		boolean efeitoAplicado = false;

		if (aplicaSangramento && aplicaVeneno) {
			// Causa Toxina (Grau 2: 4 ticks × 50%)
			System.out.println(">>> SUCESSO! Aplica [Toxina]!");
			Efeito toxina = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Toxina", 0, danoDaSource);
			alvo.adicionarEfeito(toxina);
			efeitoAplicado = true;

		} else if (aplicaSangramento) {
			// Causa Sangramento (Grau 1: 5 ticks × 25%)
			System.out.println(">>> SUCESSO! Aplica [Sangramento]!");
			Efeito sangramento = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Sangramento", 0, danoDaSource);
			alvo.adicionarEfeito(sangramento);
			efeitoAplicado = true;

		} else if (aplicaVeneno) {
			// Causa Veneno (Grau 1: 6 ticks × 20%)
			System.out.println(">>> SUCESSO! Aplica [Veneno]!");
			Efeito veneno = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Veneno", 0, danoDaSource);
			alvo.adicionarEfeito(veneno);
			efeitoAplicado = true;
		}

		// Recalcula os stats do alvo se um efeito foi aplicado (para a UI mostrar o debuff)
		if (efeitoAplicado) {
			alvo.recalcularAtributosEstatisticas();
		}
	}
}