package com.github.naton1.jvmexplorer.bytecode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;

@Slf4j
@RequiredArgsConstructor
public class JasmAssembler implements Assembler {

	private final String sourceName;

	@Override
	public byte[] assemble(String text) throws AssemblyException {
		try {
			final com.roscopeco.jasm.JasmAssembler assembler = new com.roscopeco.jasm.JasmAssembler(sourceName,
			                                                                                        () -> new ByteArrayInputStream(
					                                                                                        text.getBytes()));
			return assembler.assemble();
		}
		catch (Throwable e) {
			log.warn("Assembly failed", e);
			throw new AssemblyException(e);
		}
	}

}
