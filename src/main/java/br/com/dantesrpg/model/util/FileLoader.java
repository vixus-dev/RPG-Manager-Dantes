package br.com.dantesrpg.model.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

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

		// Se falhar, tenta achar de forma case-insensitive no classpath (necessário para o JAR ZIP)
		if (is == null) {
			try {
				String caminhoNormalizado = caminhoRelativo.replace("\\", "/");
				int ultimoSlash = caminhoNormalizado.lastIndexOf('/');
				if (ultimoSlash != -1) {
					String diretorioPai = caminhoNormalizado.substring(0, ultimoSlash + 1);
					String nomeArquivoProcurado = caminhoNormalizado.substring(ultimoSlash + 1);

					List<String> arquivosNaPasta = listarArquivosDeDiretorio(diretorioPai, null);
					for (String arquivoReal : arquivosNaPasta) {
						if (arquivoReal.equalsIgnoreCase(nomeArquivoProcurado)) {
							String caminhoCorreto = diretorioPai + arquivoReal;
							is = FileLoader.class.getResourceAsStream(caminhoCorreto);
							if (is != null) {
								System.out.println("[FileLoader] Resolvido case-insensivel no JAR: " + caminhoRelativo + " -> " + caminhoCorreto);
								break;
							}
						}
					}
				}
			} catch (Exception e) {
				// ignore
			}
		}

		if (is == null) {
			System.err.println("[FileLoader] ERRO CRÍTICO: Arquivo não encontrado em lugar nenhum: " + caminhoRelativo);
		}

		return is;
	}

	/**
	 * Lista nomes de arquivos de um diretório no classpath (funciona tanto no disco de dev
	 * quanto compactado dentro do JAR executável).
	 *
	 * @param caminhoDiretorio O caminho relativo no classpath (ex: "/data/Lojas/")
	 * @param sufixo Filtro por sufixo opcional (ex: ".json")
	 * @return Lista de nomes de arquivos (sem o caminho completo)
	 */
	public static List<String> listarArquivosDeDiretorio(String caminhoDiretorio, String sufixo) {
		return listarArquivosDeDiretorio(caminhoDiretorio, sufixo, false);
	}

	/**
	 * Lista arquivos de um diretório no classpath com opção recursiva.
	 *
	 * @param caminhoDiretorio O caminho relativo no classpath (ex: "/mapas/")
	 * @param sufixo Filtro por sufixo opcional (ex: ".png")
	 * @param recursivo Se true, varre subdiretórios recursivamente e retorna caminhos relativos ao diretório base
	 * @return Lista de caminhos relativos ao caminhoDiretorio
	 */
	public static List<String> listarArquivosDeDiretorio(String caminhoDiretorio, String sufixo, boolean recursivo) {
		List<String> resultados = new ArrayList<>();
		String pastaBusca = caminhoDiretorio;
		if (!pastaBusca.startsWith("/")) {
			pastaBusca = "/" + pastaBusca;
		}
		if (!pastaBusca.endsWith("/")) {
			pastaBusca = pastaBusca + "/";
		}

		// 1. TENTA LER DO DISCO FÍSICO DE DEV PRIMEIRO (Evita abrir o ZIP se estamos no IDE)
		if (diretoriosDevValidos == null) {
			diretoriosDevValidos = new ArrayList<>();
			String[] candidatos = { "src/main/resources/", "resources/", "src/" };
			for (String candidato : candidatos) {
				if (new File(candidato).isDirectory()) {
					diretoriosDevValidos.add(candidato);
				}
			}
		}

		String caminhoLimpo = pastaBusca.substring(1);
		for (String devDir : diretoriosDevValidos) {
			File pastaDev = new File(devDir + caminhoLimpo);
			if (pastaDev.exists() && pastaDev.isDirectory()) {
				if (recursivo) {
					adicionarArquivosRecursivamenteDev(pastaDev, "", sufixo, resultados);
				} else {
					File[] files = pastaDev.listFiles();
					if (files != null) {
						for (File f : files) {
							if (f.isFile() && (sufixo == null || f.getName().toLowerCase().endsWith(sufixo.toLowerCase()))) {
								resultados.add(f.getName());
							}
						}
					}
				}
				if (!resultados.isEmpty()) {
					return resultados;
				}
			}
		}

		// 2. SE NÃO ACHOU NO DEV OU RETORNOU VAZIO, TENTA PELO CLASSPATH (dentro do JAR)
		try {
			URL url = FileLoader.class.getResource(pastaBusca);
			if (url != null) {
				URI uri = url.toURI();
				Path meuPath;

				if (uri.getScheme().equals("jar")) {
					// Rodando dentro do JAR: precisamos abrir/obter o FileSystem virtual
					FileSystem fs;
					try {
						fs = FileSystems.getFileSystem(uri);
					} catch (FileSystemNotFoundException e) {
						fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
					}
					meuPath = fs.getPath(pastaBusca);
				} else {
					meuPath = Paths.get(uri);
				}

				int maxDepth = recursivo ? Integer.MAX_VALUE : 1;
				try (Stream<Path> walk = Files.walk(meuPath, maxDepth)) {
					walk.forEach(p -> {
						if (Files.isRegularFile(p)) {
							String nomeRelativo = meuPath.relativize(p).toString().replace("\\", "/");
							if (sufixo == null || nomeRelativo.toLowerCase().endsWith(sufixo.toLowerCase())) {
								resultados.add(nomeRelativo);
							}
						}
					});
				}
			}
		} catch (Exception e) {
			System.err.println("[FileLoader] Erro ao listar arquivos do classpath em " + pastaBusca + ": " + e.getMessage());
		}

		return resultados;
	}

	private static void adicionarArquivosRecursivamenteDev(File pasta, String prefixo, String sufixo, List<String> resultados) {
		File[] files = pasta.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					adicionarArquivosRecursivamenteDev(f, prefixo + f.getName() + "/", sufixo, resultados);
				} else {
					if (sufixo == null || f.getName().toLowerCase().endsWith(sufixo.toLowerCase())) {
						resultados.add(prefixo + f.getName());
					}
				}
			}
		}
	}
}