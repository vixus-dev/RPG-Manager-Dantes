package br.com.dantesrpg.model.personagem;

import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoEfeito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Gerencia efeitos ativos (buffs, debuffs, DoTs) de um Personagem.
 * Lógica de imunidade, acúmulo de Sono/Charm, controle mental e expiração.
 */
public class PersonagemEffects {

	private final Personagem personagem;

	public PersonagemEffects(Personagem personagem) {
		this.personagem = personagem;
	}

	// ========== CONSULTA ==========

	public Map<String, Efeito> getEfeitosAtivos() {
		Map<String, Efeito> mapa = personagem.getEfeitosAtivosMutavel();
		return mapa != null ? Collections.unmodifiableMap(mapa) : Collections.emptyMap();
	}

	// ========== ADICIONAR EFEITO ==========

	public void adicionarEfeito(Efeito efeito) {
		Map<String, Efeito> efeitosAtivos = personagem.getEfeitosAtivosMutavel();
		if (efeito == null || efeitosAtivos == null)
			return;

		// Imunidade DoT
		if (efeito.getTipo() == TipoEfeito.DOT) {
			if (efeitosAtivos.containsKey("Bênção da Vigília") || efeitosAtivos.containsKey("JACKPOT!")) {
				System.out.println(">>> IMUNIDADE! " + personagem.getNome() + " resistiu ao efeito [" + efeito.getNome() + "].");
				return;
			}
		}

		// Lógica especial: Sono
		if (efeito.getNome().equalsIgnoreCase("Sono")) {
			processarSono(efeitosAtivos);
			return;
		}

		// Lógica Genérica de Atualização/Acúmulo
		if (efeitosAtivos.containsKey(efeito.getNome())) {
			Efeito existente = efeitosAtivos.get(efeito.getNome());

			// Renova duração (pega a maior)
			existente.setDuracaoTURestante(Math.max(existente.getDuracaoTURestante(), efeito.getDuracaoTURestante()));

			// Soma stacks (Crucial para Charm e Half-Demon)
			if (efeito.getStacks() > 0) {
				existente.setStacks(existente.getStacks() + efeito.getStacks());
			}

			// Acumula escudo de sangue outorgado caso o efeito seja renovado
			if (efeito.getEscudoSangueOutorgado() > 0) {
				existente.setEscudoSangueOutorgado(existente.getEscudoSangueOutorgado() + efeito.getEscudoSangueOutorgado());
			}
		} else {
			// Adiciona novo
			efeitosAtivos.put(efeito.getNome(), efeito);
		}

		System.out.println("DEBUG [" + personagem.getNome() + "]: Efeito aplicado: " + efeito.getNome());

		// Hook: notifica a raça sobre novo efeito
		if (personagem.getRaca() != null) {
			personagem.getRaca().onEffectUpdate(personagem, efeito, true);
		}

		personagem.recalcularAtributosEstatisticas();
	}

	private void processarSono(Map<String, Efeito> efeitosAtivos) {
		// Se já está dormindo, não acumula mais sono
		if (efeitosAtivos.containsKey("Dormindo"))
			return;

		Efeito sonoAtual = efeitosAtivos.get("Sono");
		int stacksAtuais = (sonoAtual != null) ? sonoAtual.getStacks() : 0;

		int novosStacks = stacksAtuais + 1;

		if (novosStacks >= 2) {
			// ESTOUROU: Vira "Dormindo" (300 TU, acorda com 2 ticks de dano)
			removerEfeito("Sono");

			Efeito dormindo = new Efeito("Dormindo", TipoEfeito.DEBUFF, 300, null, 0, 0);
			dormindo.setStacks(0);

			efeitosAtivos.put("Dormindo", dormindo);
			System.out.println(">>> " + personagem.getNome() + " caiu no sono profundo! (300 TU ou 2 hits de dano)");

		} else {
			if (sonoAtual != null) {
				sonoAtual.setStacks(novosStacks);
			} else {
				Efeito novoSono = new Efeito("Sono", TipoEfeito.DEBUFF, 99999, null, 0, 0);
				novoSono.setStacks(novosStacks);
				efeitosAtivos.put("Sono", novoSono);
			}
			System.out.println(">>> " + personagem.getNome() + " está sonolento (" + novosStacks + "/2).");
		}

		personagem.recalcularAtributosEstatisticas();
	}

	// ========== REMOVER EFEITO ==========

	public Efeito removerEfeito(String nomeEfeito) {
		Map<String, Efeito> efeitosAtivos = personagem.getEfeitosAtivosMutavel();
		if (efeitosAtivos.containsKey(nomeEfeito)) {
			Efeito removido = efeitosAtivos.remove(nomeEfeito);

			if ("Controle Mental".equals(nomeEfeito)) {
				reverterControleMental();
			}

			if ("Choque".equals(nomeEfeito)) {
				if (removido.getModificadores() != null && removido.getModificadores().containsKey("TU_ADICIONADO")) {
					double tuExtra = removido.getModificadores().get("TU_ADICIONADO");
					personagem.setContadorTU(personagem.getContadorTU() + (int) tuExtra);
					System.out.println(">>> CHOQUE PROCOU! +" + (int) tuExtra + " TU em " + personagem.getNome());

}
			}

			if ("Fortificar".equals(nomeEfeito)) {
				double outorgado = removido.getEscudoSangueOutorgado();
				if (outorgado > 0) {
					double atual = personagem.getEscudoSangueAtual();
					double restante = Math.min(outorgado, atual);
					if (restante > 0) {
						personagem.setEscudoSangueAtual(atual - restante);
						if (personagem.isVivo()) {
							double cura = restante / 2.0;
							personagem.setVidaAtual(personagem.getVidaAtual() + cura);

}
					}
				}
			}

			if (personagem.getRaca() != null) {
				personagem.getRaca().onEffectUpdate(personagem, removido, false);
			}

			personagem.recalcularAtributosEstatisticas();
			return removido;
		}
		return null;
	}

	// ========== CONTROLE MENTAL ==========

	private void reverterControleMental() {
		String faccaoOriginal = null;
		String propParaRemover = null;

		for (String prop : personagem.getPropriedades()) {
			if (prop.startsWith("ORIGINAL_FACTION:")) {
				faccaoOriginal = prop.split(":")[1];
				propParaRemover = prop;
				break;
			}
		}

		if (faccaoOriginal != null) {
			personagem.setFaccao(faccaoOriginal);
			personagem.getPropriedades().remove(propParaRemover);
			System.out.println(">>> " + personagem.getNome() + " recobrou a consciência! Voltou para: " + faccaoOriginal);
		}
	}

	// ========== REDUZIR DURAÇÃO ==========

	public void reduzirDuracaoEfeitos(int tempoDecorrido) {
		Map<String, Efeito> efeitosAtivos = personagem.getEfeitosAtivosMutavel();
		List<String> paraRemover = new ArrayList<>();

		for (Efeito e : efeitosAtivos.values()) {
			if (!e.getNome().equals("Charm")) { // Charm não expira por tempo
				e.reduzirDuracao(tempoDecorrido);
				if (e.expirou()) {
					paraRemover.add(e.getNome());
				}
			}
		}

		for (String nome : paraRemover) {
			System.out.println(">>> Efeito expirou: " + nome);
			removerEfeito(nome);
		}
	}
}
