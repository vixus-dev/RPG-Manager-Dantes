package br.com.dantesrpg.model.util;

import br.com.dantesrpg.model.Efeito;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centraliza a construção de tooltips detalhados para todos os efeitos do jogo.
 */
public class EffectTooltipBuilder {

	private static final Map<String, String> DESCRICOES = new LinkedHashMap<>();
	private static final Map<String, String> NOMES_MODIFICADORES = new LinkedHashMap<>();

	static {
		// ===================== DESCRICOES DOS EFEITOS =====================

		// --- DoTs ---
		DESCRICOES.put("Toxina", "Veneno rápido que causa 50% do dano como dano contínuo a cada 50 TU.");
		DESCRICOES.put("Sangramento", "Ferida aberta que causa 25% do dano como dano contínuo a cada 100 TU.");
		DESCRICOES.put("Veneno", "Toxina persistente que causa 20% do dano como dano contínuo a cada 100 TU.");
		DESCRICOES.put("Queimação", "Chamas que causam 30% do dano como dano contínuo a cada 100 TU.");
		DESCRICOES.put("HellFire",
				"Fogo infernal devastador. Causa 15% do dano por tick e reduz a cura em +2% a cada tick (acumula).");
		DESCRICOES.put("Hemorragia",
				"Hemorragia grave que causa 2% da vida máxima por tick e reduz cura recebida em 30%.");
		DESCRICOES.put("Chama Divina",
				"Chamas divinas que causam 50% do dano contínuo por tick. Cada tick reduz a armadura em 20% e a cura em 50%.");

		// --- Debuffs CC ---
		DESCRICOES.put("Choque", "Sobrecarga elétrica que adiciona +20 TU ao alvo no momento da aplicação.");
		DESCRICOES.put("Pesadelo",
				"Só pode ser aplicado em alvos dormindo. Acorda o alvo e aumenta o dano recebido em 60%.");
		DESCRICOES.put("Sono",
				"Acumula sonolência. 5 acúmulos = Dormindo (300 TU). O alvo acorda após receber 2 ticks de dano.");
		DESCRICOES.put("STUN", "Atordoa o alvo, fazendo-o perder o próximo turno.");
		DESCRICOES.put("Lento", "Retarda o alvo, aumentando o custo em TU de todas as habilidades em +30%.");
		DESCRICOES.put("Charm",
				"Acúmulos baseados em Carisma do atacante. Ao atingir 100 acúmulos, o alvo entra em transe por 100 TU.");

		// --- Debuffs de Redução de Cura ---
		DESCRICOES.put("Ruptura", "Reduz a cura recebida em 50% e enfraquece a defesa.");
		DESCRICOES.put("Dilaceramento", "Ferida profunda que reduz a cura recebida em 25%.");
		DESCRICOES.put("Corta Cura", "Reduz a cura recebida pelo alvo em 25%.");
		DESCRICOES.put("Corta Cura+", "Versão potente. Reduz a cura recebida pelo alvo em 40%.");

		// --- Outros Debuffs ---
		DESCRICOES.put("Marca do Deserto", "Marca mística que enfraquece o alvo.");
		DESCRICOES.put("Armadura Quebrada", "A armadura do alvo foi comprometida, reduzindo sua redução de dano em 30%.");

		// --- Buffs de Status ---
		DESCRICOES.put("Força Ampliada (+20% Dano)", "Amplia a força, concedendo +20% de dano em todos os ataques.");
		DESCRICOES.put("Força Ampliada V2 (+40% Dano)",
				"Amplia enormemente a força, concedendo +40% de dano em todos os ataques.");
		DESCRICOES.put("Escudo Divino (+Armadura)", "Proteção divina que aumenta a armadura total.");
		DESCRICOES.put("Velocidade (+Movimento)", "Aumenta a velocidade de movimento em +2 casas.");

		// --- Buffs de Classe ---
		DESCRICOES.put("Stealth",
				"O personagem está invisível. Não pode ser alvo de ataques diretos. Quebra ao atacar.");
		DESCRICOES.put("Bênção da Vigília", "Bênção sagrada que concede +20 de armadura total.");
		DESCRICOES.put("Tiro Especial", "Prepara um tiro potencializado com efeito especial no próximo ataque.");
		DESCRICOES.put("Combo!", "Encadeamento de golpes! Modifica o próximo ataque ou habilidade.");
		DESCRICOES.put("Restrição Celestial",
				"Selo celestial que restringe o alvo, impedindo ações específicas.");
		DESCRICOES.put("Guardião",
				"Postura defensiva. O personagem protege aliados adjacentes, interceptando ataques.");
		DESCRICOES.put("Escudo do Campeão", "Escudo heróico que concede +20 de armadura total.");

		// --- Efeitos de Combate (CombatManager) ---
		DESCRICOES.put("Vento Congelante", "Rajada congelante que retarda o alvo.");
		DESCRICOES.put("Controle Mental", "O alvo está sob controle mental e age contra seus aliados.");
		DESCRICOES.put("Bênção da Justiça", "Aura de justiça que fortalece aliados próximos com bônus de dano e defesa.");
		DESCRICOES.put("O Vazio", "Aura opressora que enfraquece todos próximos, reduzindo seus atributos.");
		DESCRICOES.put("Ringue da Vontade",
				"Campo de combate 7x7 fechado (400 TU). Ninguém entra ou sai. Passivas do Punho Infinito amplificadas dentro da área.");
		DESCRICOES.put("Modo Engaged", "Modo de combate engajado. Modificadores aumentados pela arma.");
		DESCRICOES.put("Dormindo", "O alvo está dormindo. Ações bloqueadas por 300 TU. Acorda após 2 ticks de dano.");

		// --- Efeitos de Fantasmas Nobres ---
		DESCRICOES.put("Domínio: Idle Death Gamble (Preparando)",
				"Preparando o domínio. Apostas estão sendo calculadas...");
		DESCRICOES.put("Domínio: Idle Death Gamble",
				"Domínio 7x7 ativo (300 TU). -50% dano e -25% custo TU dentro da área. Cada ação rola 3 dados — trinca ou 6+ estrelas = JACKPOT!");
		DESCRICOES.put("JACKPOT!", "JACKPOT! Bônus massivo temporário após uma aposta vitoriosa.");
		DESCRICOES.put("Estrelas da Sorte", "Estrelas da sorte acumuladas. 6+ estrelas garantem JACKPOT na próxima aposta.");
		DESCRICOES.put("Modo Justiça",
				"Modo Justiça ativado! Aura que fortalece aliados e enfraquece inimigos próximos.");
		DESCRICOES.put("Vigília (Foco)", "Foco concentrado da Vigília. Bônus especial para o portador.");
		DESCRICOES.put("Ringue (Preparando)", "Preparando o Ringue da Vontade. O campo será selado em breve.");

		// --- Efeitos Raciais ---
		DESCRICOES.put("Τέλεια αρμονία",
				"Harmonia Perfeita (Anjo Caído). Estado transcendente com bônus massivos a todos os atributos. Concede voo.");
		DESCRICOES.put("Voo", "O personagem está voando. Ignora terreno e obstáculos terrestres.");
		DESCRICOES.put("Estado Dourado", "Estado Dourado (Elfo). Momento de poder concentrado.");
		DESCRICOES.put("Ascensão", "Ascensão (Meio Anjo). Forma angelical com atributos amplificados.");
		DESCRICOES.put("Sobrecarga Demoníaca",
				"Explosão momentânea de poder demoníaco. Bônus temporário de atributos.");
		DESCRICOES.put("Forma Demoníaca",
				"Transformação demoníaca completa. Atributos massivamente amplificados enquanto durar.");
		DESCRICOES.put("Empréstimo", "Empréstimo de vida (Humano). Vida negativa temporária que precisa ser paga.");
		DESCRICOES.put("Contrato de Vida",
				"Contrato de Vida (Humano). Parte da vida máxima está comprometida pela dívida.");
		DESCRICOES.put("Forma Lupina",
				"Transformação em lobo. Atributos físicos amplificados, instintos aguçados.");

		// --- Efeitos de Habilidades de Classe ---
		DESCRICOES.put("Poder das Sombras", "Sombra invocada recebe bônus de poder temporário.");
		DESCRICOES.put("Monarca das Sombras", "O invocador é fortalecido por suas sombras. Bônus enquanto elas existirem.");
		DESCRICOES.put("Maldição Umbraum", "Maldição sombria que causa dano contínuo ao alvo.");
		DESCRICOES.put("Angry Again", "Fúria renovada! Bônus de dano temporário após receber dano.");
		DESCRICOES.put("Ouvidos Sangrando", "O alvo está atordoado pelo som. Atributos reduzidos.");
		DESCRICOES.put("Grito de Guerra", "Grito inspirador que concede bônus de dano a aliados próximos.");
		DESCRICOES.put("Tecnica de Amplificação", "Amplificação mágica. Bônus temporário de poder.");
		DESCRICOES.put("Campo De Força", "Barreira mágica que absorve dano como escudo.");
		DESCRICOES.put("Barreira de Mana", "Escudo de mana que absorve dano.");
		DESCRICOES.put("Marca do Deserto", "Marca mística do deserto. Enfraquece o alvo marcado.");
		DESCRICOES.put("Sussurro Sombrio", "Sussurro que enfraquece a mente do alvo.");
		DESCRICOES.put("Seek and Destroy", "Alvo localizado! Bônus de dano contra o inimigo marcado.");
		DESCRICOES.put("Adaptando", "Adaptação em progresso. Modificadores sendo ajustados continuamente.");
		DESCRICOES.put("Enfraquecido (-30%)", "O alvo foi enfraquecido. Redução de dano em 30%.");
		DESCRICOES.put("ruptura+", "Ruptura severa. Redução de armadura e cura do alvo.");
		DESCRICOES.put("Drenagem de Efeitos", "Efeitos drenados do inimigo convertidos em bônus para o usuário.");
		DESCRICOES.put("Aura do Zero", "Aura opressora do Zero. Enfraquece todos ao redor.");

		// ===================== NOMES LEGÍVEIS DOS MODIFICADORES =====================
		NOMES_MODIFICADORES.put("DANO_BONUS_PERCENTUAL", "Dano Bônus");
		NOMES_MODIFICADORES.put("ARMADURA_TOTAL", "Armadura");
		NOMES_MODIFICADORES.put("BONUS_ARMADURA_PERCENTUAL", "Bônus de Armadura");
		NOMES_MODIFICADORES.put("MOVIMENTO", "Movimento");
		NOMES_MODIFICADORES.put("REDUCAO_CURA", "Redução de Cura");
		NOMES_MODIFICADORES.put("REDUCAO_DANO_MODIFICADOR", "Redução de Dano");
		NOMES_MODIFICADORES.put("PERCENTUAL_HP_MAX", "% Vida Máx/tick");
		NOMES_MODIFICADORES.put("CUSTO_TU_PERCENTUAL", "Custo de TU Extra");
		NOMES_MODIFICADORES.put("FORCA", "Força");
		NOMES_MODIFICADORES.put("DESTREZA", "Destreza");
		NOMES_MODIFICADORES.put("INTELIGENCIA", "Inteligência");
		NOMES_MODIFICADORES.put("CONSTITUICAO", "Constituição");
		NOMES_MODIFICADORES.put("CARISMA", "Carisma");
		NOMES_MODIFICADORES.put("TAXA_CRITICA", "Taxa Crítica");
	}

	/**
	 * Constrói um tooltip completo e detalhado para um efeito.
	 */
	public static String buildTooltip(Efeito efeito) {
		if (efeito == null)
			return "";

		StringBuilder sb = new StringBuilder();

		// === NOME + TIPO ===
		String tipoLabel;
		switch (efeito.getTipo()) {
		case BUFF:
			tipoLabel = "BUFF";
			break;
		case DEBUFF:
			tipoLabel = "DEBUFF";
			break;
		case DOT:
			tipoLabel = "DANO CONTÍNUO";
			break;
		default:
			tipoLabel = "EFEITO";
		}
		sb.append("【").append(efeito.getNome()).append("】 — ").append(tipoLabel).append("\n");

		// === DESCRIÇÃO ===
		String descricao = getDescricao(efeito.getNome());
		if (descricao != null) {
			sb.append(descricao).append("\n");
		}

		sb.append("─────────────────────\n");

		// === DURAÇÃO ===
		if (efeito.getDuracaoTUInicial() >= 99999) {
			sb.append("Duração: Permanente (por acúmulo)\n");
		} else {
			sb.append("Duração: ").append(efeito.getDuracaoTURestante()).append(" / ")
					.append(efeito.getDuracaoTUInicial()).append(" TU\n");
		}

		// === STACKS ===
		if (efeito.getStacks() > 0) {
			sb.append("Acúmulos: ").append(efeito.getStacks()).append("\n");
		}

		// === DANO POR TICK ===
		if (efeito.getDanoPorTick() > 0) {
			sb.append("Dano/Tick: ").append(efeito.getDanoPorTick());
			if (efeito.getIntervaloTickTU() > 0) {
				sb.append(" a cada ").append(efeito.getIntervaloTickTU()).append(" TU");
			}
			sb.append("\n");

			// Ticks restantes
			if (efeito.getIntervaloTickTU() > 0) {
				int ticksRestantes = efeito.getDuracaoTURestante() / efeito.getIntervaloTickTU();
				int danoTotalRestante = ticksRestantes * efeito.getDanoPorTick();
				sb.append("Ticks restantes: ").append(ticksRestantes);
				sb.append(" (≈").append(danoTotalRestante).append(" dano total)\n");
			}
		}

		// === MODIFICADORES ===
		if (efeito.getModificadores() != null && !efeito.getModificadores().isEmpty()) {
			for (Map.Entry<String, Double> mod : efeito.getModificadores().entrySet()) {
				String nomeModificador = NOMES_MODIFICADORES.getOrDefault(mod.getKey(), mod.getKey());
				double valor = mod.getValue();

				sb.append("• ").append(nomeModificador).append(": ");

				// Formatação inteligente
				if (mod.getKey().contains("PERCENTUAL") || mod.getKey().contains("REDUCAO")
						|| mod.getKey().contains("TAXA") || mod.getKey().contains("BONUS_PERCENTUAL")) {
					// Valores percentuais
					int pct = (int) Math.round(valor * 100);
					sb.append(pct > 0 ? "+" : "").append(pct).append("%");
				} else {
					// Valores absolutos
					int intVal = (int) valor;
					sb.append(intVal > 0 ? "+" : "").append(intVal);
				}
				sb.append("\n");
			}
		}

		return sb.toString().trim();
	}

	/**
	 * Busca a descrição do efeito pelo nome, com fallback para nomes parciais.
	 */
	private static String getDescricao(String nomeEfeito) {
		if (nomeEfeito == null)
			return null;

		// Busca direta
		if (DESCRICOES.containsKey(nomeEfeito))
			return DESCRICOES.get(nomeEfeito);

		// Busca parcial (para efeitos com prefixo como "CD:NomeHabilidade")
		if (nomeEfeito.startsWith("CD:")) {
			return "Cooldown da habilidade " + nomeEfeito.substring(3).trim() + ".";
		}

		// Busca por substring
		for (Map.Entry<String, String> entry : DESCRICOES.entrySet()) {
			if (nomeEfeito.contains(entry.getKey()) || entry.getKey().contains(nomeEfeito)) {
				return entry.getValue();
			}
		}

		return null;
	}
}
