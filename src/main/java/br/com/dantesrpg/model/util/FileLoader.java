package br.com.dantesrpg.model.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class FileLoader {

	// Cache do diretório de desenvolvimento resolvido na primeira chamada
	private static String diretorioDevResolvido = null;
	private static boolean diretorioDevJaResolvido = false;

	public static InputStream carregarArquivo(String caminhoRelativo) {
		try {
			String caminhoLimpo = caminhoRelativo.startsWith("/") ? caminhoRelativo.substring(1) : caminhoRelativo;

			// Resolve o diretório de desenvolvimento apenas UMA VEZ
			if (!diretorioDevJaResolvido) {
				if (new File("src/main/resources/").isDirectory()) {
					diretorioDevResolvido = "src/main/resources/";
				} else if (new File("resources/").isDirectory()) {
					diretorioDevResolvido = "resources/";
				} else if (new File("src/").isDirectory()) {
					diretorioDevResolvido = "src/";
				}
				diretorioDevJaResolvido = true;
			}

			if (diretorioDevResolvido != null) {
				File arquivo = new File(diretorioDevResolvido + caminhoLimpo);
				if (arquivo.exists()) {
					return new FileInputStream(arquivo);
				}
			}

		} catch (Exception e) {
			// Se der erro ao tentar ler do disco, ignoramos e vamos para o fallback
			System.out.println("[FileLoader] Erro ao ler do disco, tentando classpath...");
		}

		// 2. FALLBACK: TENTA LER DO CLASSPATH (Binário/Compilado)
		InputStream is = FileLoader.class.getResourceAsStream(caminhoRelativo);

		if (is == null) {
			System.err.println("[FileLoader] ERRO CRÍTICO: Arquivo não encontrado em lugar nenhum: " + caminhoRelativo);
		}

		return is;
	}
}