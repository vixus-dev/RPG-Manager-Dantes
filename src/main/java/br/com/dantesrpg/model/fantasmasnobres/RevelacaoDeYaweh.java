package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.armas.unicas.Yaweh;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.habilidades.ArremessoDeEscudo;
import br.com.dantesrpg.model.habilidades.PosturaDeBloqueio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Despertar Divino — Fantasma Nobre de Sarvant.
 *
 * Custo: 120 TU, 2 Mana. Cooldown: 800 TU.
 * Requisito: HP abaixo de 50% do máximo.
 *
 * Ao ativar, o personagem se transforma por 800 TU:
 * - As habilidades do grimório Yaweh somem temporariamente
 * - Yaweh passa a ter alcance 1, custoTU 120, escala com Inspiração, e ataque alternativo em cone 100° (1.2x dano, alcance 2)
 * - Chama Divina: 40% de chance, todos os inimigos (não só demônios)
 * - Escudo "Proteção dos Céus" = 5 × INSPIRAÇÃO (regenera 50% do perdido no turno, ou +5% crit se intacto)
 * - Concede: Postura de Bloqueio e Arremesso de Escudo
 */
public class RevelacaoDeYaweh extends FantasmaNobre {

	public static final String EFEITO_DESPERTAR = "Despertar Divino";
	public static final String PROP_DESPERTAR_ATIVO = "DESPERTAR_DIVINO_ATIVO";
	private static final int DURACAO_TRANSFORMACAO = 800;

	// Backup do estado original do Yaweh
	private static final String PROP_YAWEH_ALCANCE_ORIGINAL = "YAWEH_ALCANCE_ORIGINAL";
	private static final String PROP_YAWEH_CUSTO_ORIGINAL = "YAWEH_CUSTO_ORIGINAL";

	@Override
	public String getNome() {
		return "Despertar Divino";
	}

	@Override
	public String getDescricao() {
		return "Sarvant desperta o poder divino de Yaweh. Requer HP abaixo de 50%. "
				+ "Transforma a arma, concede escudo divino (5×INSP), Chama Divina aprimorada (40%, todos os inimigos), "
				+ "e habilidades: Postura de Bloqueio e Arremesso de Escudo.";
	}

	@Override
	public int getCustoMana() {
		return 2;
	}

	@Override
	public int getCustoTU() {
		return 120;
	}

	@Override
	public int getCooldownTU() {
		return 800;
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		return TipoAlvo.SI_MESMO;
	}

	@Override
	public int getTamanhoArea() {
		return 0;
	}

	@Override
	public int getNumeroDeAlvos() {
		return 0;
	}

	@Override
	public String getMotivoBloqueio(Personagem conjurador) {
		if (conjurador == null) {
			return "Fantasma Nobre indisponível.";
		}
		if (conjurador.getEfeitosAtivos().containsKey(EFEITO_DESPERTAR)) {
			return conjurador.getNome() + " já está transformado em Despertar Divino.";
		}
		if (conjurador.getVidaAtual() >= conjurador.getVidaMaxima() * 0.50) {
			return conjurador.getNome() + " precisa estar abaixo de 50% de HP para ativar o Despertar Divino.";
		}
		return null;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			AcaoMestreInput input, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " ativa o Fantasma Nobre: DESPERTAR DIVINO!");

		// 1. Aplicar efeito marcador de transformação (800 TU)
		Map<String, Double> mods = new HashMap<>();
		mods.put("TAXA_CRITICA", 0.0); // Será incrementado por turno se escudo intacto
		Efeito efeitoDespertar = new Efeito(EFEITO_DESPERTAR, TipoEfeito.BUFF, DURACAO_TRANSFORMACAO, mods, 0, 0);
		manager.aplicarEfeito(conjurador, efeitoDespertar);

		// 2. Adicionar flag para o EffectProcessor (Chama Divina aprimorada)
		conjurador.adicionarPropriedade(PROP_DESPERTAR_ATIVO);

		// 3. Modificar a arma Yaweh
		Arma arma = obterYaweh(conjurador);
		if (arma != null) {
			// Salvar estado original
			conjurador.adicionarPropriedade(PROP_YAWEH_ALCANCE_ORIGINAL + ":" + arma.getAlcance());
			conjurador.adicionarPropriedade(PROP_YAWEH_CUSTO_ORIGINAL + ":" + arma.getCustoTU());

			// Aplicar novos valores: alcance 1, custoTU 120
			arma.setCustoTU(120);
			// Yaweh é um Grimorio que usa Atributo.INSPIRACAO — mantém.
			// Alcance não tem setter na Arma base. Vamos usar o Yaweh.setAlcanceDespertar(1)
			if (arma instanceof Yaweh) {
				((Yaweh) arma).ativarDespertarDivino();
			}

			// Remover magias do grimório temporariamente (serão restauradas ao reverter)
			if (arma instanceof Grimorio) {
				Grimorio grimorio = (Grimorio) arma;
				// Salvar nomes das magias para restauração
				List<String> nomesMagias = new ArrayList<>();
				for (Habilidade h : grimorio.getMagiasArmazenadas()) {
					nomesMagias.add(h.getNome());
				}
				conjurador.adicionarPropriedade("YAWEH_MAGIAS_BACKUP:" + String.join(",", nomesMagias));
				grimorio.getMagiasArmazenadas().clear();
			}
		}

		// 4. Adicionar habilidades da transformação
		conjurador.adicionarHabilidadeExtra(new PosturaDeBloqueio());
		conjurador.adicionarHabilidadeExtra(new ArremessoDeEscudo());
		System.out.println(">>> Habilidades concedidas: Postura de Bloqueio, Arremesso de Escudo.");

		// 5. Configurar Escudo Divino (5 × INSPIRAÇÃO)
		int inspiracao = conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 1);
		double valorEscudo = inspiracao * 5.0;
		conjurador.setEscudoDivinoMaximo(valorEscudo);
		conjurador.setEscudoDivinoAtual(valorEscudo);

		// Inicializar propriedade para tracking do escudo
		conjurador.getPropriedades().removeIf(prop -> prop.startsWith("DespertarEscudoLastHP:"));
		conjurador.adicionarPropriedade("DespertarEscudoLastHP:" + (int) valorEscudo);

		System.out.println(">>> Escudo Divino: " + (int) valorEscudo + " HP (5 × " + inspiracao + " INSP).");

		conjurador.recalcularAtributosEstatisticas();
	}

	@Override
	public void onTurnStart(Personagem conjurador, EstadoCombate estado, CombatManager manager) {
		if (!conjurador.getEfeitosAtivos().containsKey(EFEITO_DESPERTAR)) {
			return;
		}

		int inspiracao = conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 1);
		double escudoMaximo = inspiracao * 5.0;
		double escudoAtual = conjurador.getEscudoDivinoAtual();

		// Lê o último valor do escudo para comparação
		double lastShieldValue = escudoMaximo;
		String propKey = "DespertarEscudoLastHP:";
		for (String prop : conjurador.getPropriedades()) {
			if (prop.startsWith(propKey)) {
				try {
					lastShieldValue = Double.parseDouble(prop.substring(propKey.length()));
				} catch (Exception e) { }
				break;
			}
		}

		if (escudoAtual >= escudoMaximo) {
			// Escudo no máximo → +5% taxa crítica cumulativo
			Efeito efeito = conjurador.getEfeitosAtivos().get(EFEITO_DESPERTAR);
			if (efeito != null) {
				Map<String, Double> efeitoMods = efeito.getModificadores();
				if (efeitoMods == null) {
					efeitoMods = new HashMap<>();
					efeito.setModificadores(efeitoMods);
				} else if (!(efeitoMods instanceof HashMap)) {
					efeitoMods = new HashMap<>(efeitoMods);
					efeito.setModificadores(efeitoMods);
				}
				double currentCrit = efeitoMods.getOrDefault("TAXA_CRITICA", 0.0);
				efeitoMods.put("TAXA_CRITICA", currentCrit + 0.05);
				System.out.println(">>> Despertar Divino: Escudo intacto! Taxa Crítica acumulada: +"
						+ (int) ((currentCrit + 0.05) * 100) + "%");
			}
		} else {
			// Escudo abaixo do máximo → recuperar 50% do perdido
			double perdido = escudoMaximo - escudoAtual;
			double recuperacao = perdido * 0.50;
			double novoEscudo = Math.min(escudoMaximo, escudoAtual + recuperacao);

			conjurador.setEscudoDivinoMaximo(escudoMaximo);
			conjurador.setEscudoDivinoAtual(novoEscudo);
			escudoAtual = novoEscudo;

			System.out.println(">>> Despertar Divino: Escudo danificado! Recuperou " + String.format("%.0f", recuperacao)
					+ " HP. Escudo: " + (int) novoEscudo + "/" + (int) escudoMaximo);
		}

		// Atualiza a propriedade com o novo valor do escudo
		conjurador.getPropriedades().removeIf(prop -> prop.startsWith(propKey));
		conjurador.adicionarPropriedade(propKey + (int) escudoAtual);
		conjurador.recalcularAtributosEstatisticas();
	}

	/**
	 * Reverte todas as alterações da transformação Despertar Divino.
	 */
	public static void reverterDespertarDivino(Personagem p) {
		System.out.println(">>> " + p.getNome() + " — O Despertar Divino se dissipou.");

		// Remover flag
		p.getPropriedades().remove(PROP_DESPERTAR_ATIVO);

		// Restaurar Yaweh
		Arma arma = obterYaweh(p);
		if (arma != null) {
			// Restaurar alcance e custoTU originais
			for (String prop : new ArrayList<>(p.getPropriedades())) {
				if (prop.startsWith(PROP_YAWEH_CUSTO_ORIGINAL + ":")) {
					try {
						int custoOriginal = Integer.parseInt(prop.split(":")[1]);
						arma.setCustoTU(custoOriginal);
					} catch (Exception e) { }
				}
			}

			if (arma instanceof Yaweh) {
				((Yaweh) arma).desativarDespertarDivino();
			}

			// Restaurar magias do grimório
			if (arma instanceof Grimorio) {
				Grimorio grimorio = (Grimorio) arma;
				for (String prop : new ArrayList<>(p.getPropriedades())) {
					if (prop.startsWith("YAWEH_MAGIAS_BACKUP:")) {
						String[] nomes = prop.substring("YAWEH_MAGIAS_BACKUP:".length()).split(",");
						for (String nome : nomes) {
							Habilidade h = br.com.dantesrpg.model.util.HabilidadeFactory.criarHabilidadePorNome(nome.trim());
							if (h != null) {
								grimorio.aprenderMagia(h);
							}
						}
						break;
					}
				}
			}
		}

		// Limpar propriedades de backup
		p.getPropriedades().removeIf(prop ->
				prop.startsWith(PROP_YAWEH_ALCANCE_ORIGINAL)
				|| prop.startsWith(PROP_YAWEH_CUSTO_ORIGINAL)
				|| prop.startsWith("YAWEH_MAGIAS_BACKUP:")
				|| prop.startsWith("DespertarEscudoLastHP:"));

		// Remover habilidades extras da transformação
		p.limparHabilidadesExtras();

		// Remover escudo divino
		p.setEscudoDivinoAtual(0);
		p.setEscudoDivinoMaximo(0);

		// Remover efeito de transformação (se ainda existir)
		p.removerEfeito(EFEITO_DESPERTAR);

		p.recalcularAtributosEstatisticas();
	}

	/**
	 * Busca a arma Yaweh entre as armas equipadas do personagem.
	 */
	private static Arma obterYaweh(Personagem p) {
		for (Arma a : p.getArmasEquipadas()) {
			if ("Yaweh".equalsIgnoreCase(a.getNome())) {
				return a;
			}
		}
		return null;
	}
}
