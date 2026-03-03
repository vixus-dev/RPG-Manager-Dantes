package br.com.dantesrpg.model.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class FileLoader {

	public static InputStream carregarArquivo(String caminhoRelativo) {
		try {
			String caminhoLimpo = caminhoRelativo.startsWith("/") ? caminhoRelativo.substring(1) : caminhoRelativo;

			File arquivoDev = null;

			File opcaoMaven = new File("src/main/resources/" + caminhoLimpo);

			File opcaoSimples = new File("resources/" + caminhoLimpo);
			File opcaoSrc = new File("src/" + caminhoLimpo);

			if (opcaoMaven.exists()) {
				arquivoDev = opcaoMaven;
			} else if (opcaoSimples.exists()) {
				arquivoDev = opcaoSimples;
			} else if (opcaoSrc.exists()) {
				arquivoDev = opcaoSrc;
			}

			if (arquivoDev != null) {
				return new FileInputStream(arquivoDev);
			}

		} catch (Exception e) {
			// Se der erro ao tentar ler do disco, ignoramos e vamos para o fallback
			System.out.println("[FileLoader] Erro ao ler do disco, tentando classpath...");
		}

		// 2. FALLBACK: TENTA LER DO CLASSPATH (Binário/Compilado)
		System.out.println("[FileLoader] Lendo do BIN (Compilado): " + caminhoRelativo);
		InputStream is = FileLoader.class.getResourceAsStream(caminhoRelativo);

		if (is == null) {
			System.err.println("[FileLoader] ERRO CRÍTICO: Arquivo não encontrado em lugar nenhum: " + caminhoRelativo);
		}

		return is;
	}
}