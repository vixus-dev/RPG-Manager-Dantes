package br.com.dantesrpg.model.util;

import java.text.Normalizer;
import br.com.dantesrpg.model.enums.TipoEfeito;

public class EffectIconResolver {

	/**
	 * Resolve o caminho do recurso para a imagem correspondente ao nome do efeito.
	 * Aplica regras de fallback específicas e sanitização genérica de strings.
	 *
	 * @param nomeEfeito Nome do efeito a ser resolvido
	 * @return O caminho relativo do recurso da imagem (ex: "/effects/sangramento.png")
	 */
	public static String getIconPath(String nomeEfeito) {
		if (nomeEfeito == null || nomeEfeito.trim().isEmpty()) {
			return "/effects/default.png";
		}

		String nomeOriginal = nomeEfeito.trim();
		String nomeSanitized = sanitizarNome(nomeOriginal);

		// 1. Fallback de Cooldowns (qualquer um que comece com "CD:")
		if (nomeOriginal.startsWith("CD:") || nomeSanitized.startsWith("cd_")) {
			return "/effects/cooldown.png";
		}

		// 2. Fallback de Poções (qualquer um que comece com "Buff Poção")
		if (nomeOriginal.startsWith("Buff Poção") || nomeSanitized.startsWith("buff_pocao_")) {
			return "/effects/potion.png";
		}

		// 3. Fallback de Preparação (qualquer um contendo "preparando")
		if (nomeSanitized.contains("preparando")) {
			return "/effects/preparando.png";
		}

		// 4. Fallback de Auras (qualquer um contendo "aura")
		if (nomeSanitized.contains("aura")) {
			return "/effects/aura.png";
		}

		// 5. Fallback de Gekkyūden (qualquer um contendo "gekkyuden")
		if (nomeSanitized.contains("gekkyuden")) {
			return "/effects/gekkyuden.png";
		}

		// 6. Fallback de Bênção Divina (qualquer um contendo "bencao_divina")
		if (nomeSanitized.contains("bencao_divina")) {
			return "/effects/bencao_divina.png";
		}

		// Caminho padrão do efeito resolvido
		String caminhoEfeito = "/effects/" + nomeSanitized + ".png";

		// Verifica se a imagem existe no classpath antes de retorná-la
		try (java.io.InputStream is = FileLoader.carregarArquivo(caminhoEfeito)) {
			if (is != null) {
				return caminhoEfeito;
			}
		} catch (Exception e) {
			// Ignora erro e cai no fallback genérico
		}

		// Fallback genérico final (não encontrou imagem específica)
		return "/effects/default.png";
	}

	/**
	 * Resolve o caminho do recurso para a imagem correspondente ao nome do efeito,
	 * oferecendo suporte a fallbacks genéricos diferenciados por tipo caso a imagem
	 * específica não exista.
	 *
	 * @param nomeEfeito Nome do efeito a ser resolvido
	 * @param tipo       Tipo do efeito para fallbacks genéricos ricos
	 * @return O caminho relativo do recurso da imagem
	 */
	public static String getIconPath(String nomeEfeito, TipoEfeito tipo) {
		String caminho = getIconPath(nomeEfeito);
		if ("/effects/default.png".equals(caminho) && tipo != null) {
			switch (tipo) {
				case BUFF:
					return "/effects/buff_generico.png";
				case DEBUFF:
					return "/effects/debuff_generico.png";
				case DOT:
					return "/effects/dot_generico.png";
			}
		}
		return caminho;
	}

	/**
	 * Remove acentuações, caracteres especiais, substitui espaços por sublinhados
	 * e converte para caixa baixa.
	 */
	private static String sanitizarNome(String nome) {
		if (nome == null) return "";
		// Normaliza em NFD para decompor caracteres acentuados
		String decomposta = Normalizer.normalize(nome, Normalizer.Form.NFD);
		// Remove marcas diacríticas (acentos)
		String semAcentos = decomposta.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
		// Tudo para minúsculas
		String minusculo = semAcentos.toLowerCase();
		// Substitui espaços por sublinhados
		String comUnderlines = minusculo.replace(" ", "_");
		// Remove quaisquer caracteres que não sejam letras, números ou sublinhados
		return comUnderlines.replaceAll("[^a-z0-9_]", "");
	}
}
