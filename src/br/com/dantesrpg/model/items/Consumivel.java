package br.com.dantesrpg.model.items;

import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Item;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoEfeito;

import java.util.HashMap;
import java.util.Map;

public class Consumivel extends Item {

	private String tipoId;
	private int custoTU;
	private Map<String, Double> efeitos;

	public Consumivel(String tipoId, String nome, String descricao, int valorMoedas, int custoTU,
			boolean usavelEmCombate, Map<String, Double> efeitos) {
		super(nome, descricao, valorMoedas, usavelEmCombate);
		this.tipoId = tipoId;
		this.custoTU = custoTU;
		this.efeitos = efeitos;
	}

	@Override
	public String getTipo() {
		return this.tipoId;
	}

	@Override
	public int getCustoTU() {
		return this.custoTU;
	}

	@Override
	public void usar(Personagem usuario, EstadoCombate estado,
			br.com.dantesrpg.controller.CombatController controller) {
		System.out.println(">>> " + usuario.getNome() + " usou " + getNome() + "!");

		if (efeitos == null)
			return;

		int duracaoBuff = efeitos.getOrDefault("DURACAO", 300.0).intValue();

		for (Map.Entry<String, Double> entry : efeitos.entrySet()) {
			String tipoEfeito = entry.getKey().toUpperCase();
			double valorDouble = entry.getValue();
			int valorInt = entry.getValue().intValue();

			// --- CURAS BÁSICAS ---
			switch (tipoEfeito) {
			case "CURA_HP":
				double vidaAntes = usuario.getVidaAtual();
				usuario.setVidaAtual(vidaAntes + valorDouble, estado, controller);
				double curaReal = usuario.getVidaAtual() - vidaAntes;
				System.out.println(">>> " + usuario.getNome() + " recuperou " + (int) curaReal + " HP.");
				continue; // Vai para o próximo efeito

			case "CURA_MP":
				usuario.setManaAtual(usuario.getManaAtual() + valorInt);
				System.out.println(">>> " + usuario.getNome() + " recuperou " + valorInt + " MP.");
				continue;

			case "CURA_MARIONETTE":
				if (usuario.getRaca() instanceof br.com.dantesrpg.model.racas.Marionette) {
					usuario.forcarCura(valorDouble);
					System.out.println(">>> Reparo aplicado: +" + valorInt);
				}
				continue;

			case "ESCUDO":
				usuario.setEscudoAtual(usuario.getEscudoAtual() + valorDouble);
				System.out.println(">>> " + usuario.getNome() + " ganhou " + valorInt + " de Escudo.");
				continue;

			case "DURACAO":
				continue; // Ignora, usada apenas para configuração

			case "XP":
				int xpBonus = (int) (usuario.getXpReward() * valorDouble);
				usuario.setXpReward(xpBonus);
				continue; // Ignora, usada apenas para configuração
			}

			// --- REMOÇÃO DE STATUS (Antídotos) ---
			// Chave começa com "REMOVE_" (Ex: "REMOVE_VENENO": 1.0)
			if (tipoEfeito.startsWith("REMOVE_")) {
				// Pega o nome do efeito removendo o prefixo (Ex: "VENENO")
				String statusParaRemover = tipoEfeito.replace("REMOVE_", "");

				// Tenta remover buscando keys que contenham esse nome (ignora case)
				boolean removeu = false;
				// Cria cópia das chaves para evitar erro de concorrência
				for (String efeitoAtivo : new java.util.ArrayList<>(usuario.getEfeitosAtivos().keySet())) {
					if (efeitoAtivo.toUpperCase().contains(statusParaRemover)) {
						usuario.removerEfeito(efeitoAtivo);
						System.out.println(">>> Item curou o status: " + efeitoAtivo);
						removeu = true;
					}
				}
				if (removeu)
					usuario.recalcularAtributosEstatisticas();
				continue;
			}

			// --- APLICAÇÃO DE BUFFS (Poções de Força, etc) ---
			// Chave começa com "BUFF_" (Ex: "BUFF_FORCA": 5.0)
			if (tipoEfeito.startsWith("BUFF_")) {
				String atributoAfetado = tipoEfeito.replace("BUFF_", ""); // Ex: "FORCA"

				Map<String, Double> mods = new HashMap<>();
				mods.put(atributoAfetado, valorDouble);

				// Cria um efeito temporário
				Efeito buffItem = new Efeito("Buff " + getNome(), // Nome do efeito = "Buff Poção de Força"
						TipoEfeito.BUFF, duracaoBuff, mods, 0, 0);

				usuario.adicionarEfeito(buffItem);
				usuario.recalcularAtributosEstatisticas();
				System.out.println(">>> Item aplicou Buff: +" + valorInt + " em " + atributoAfetado);
			}
		}
	}

	public Map<String, Double> getEfeitos() {
		return this.efeitos;
	}
}