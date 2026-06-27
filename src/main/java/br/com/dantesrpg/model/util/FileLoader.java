package br.com.dantesrpg.model.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FileLoader {

	// Cache dos diretórios de desenvolvimento que existem (resolvido na primeira chamada)
	private static List<String> diretoriosDevValidos = null;

	public static InputStream carregarArquivo(String caminhoRelativo) {
		try {
			String caminhoLimpo = caminhoRelativo.startsWith("/") ? caminhoRelativo.substring(1) : caminhoRelativo;

			// Resolve quais diretórios de dev existem apenas UMA VEZ
			if (diretoriosDevValidos == null) {
				diretoriosDevValidos = new ArrayList<>();
				String[] candidatos = { "src/main/resources/", "resources/", "src/" };
				for (String candidato : candidatos) {
					if (new File(candidato).isDirectory()) {
						diretoriosDevValidos.add(candidato);
					}
				}
			}

			// Tenta cada diretório de dev válido
			for (String diretorio : diretoriosDevValidos) {
				File arquivo = new File(diretorio + caminhoLimpo);
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