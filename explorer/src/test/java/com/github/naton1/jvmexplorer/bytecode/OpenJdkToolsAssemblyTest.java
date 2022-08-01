package com.github.naton1.jvmexplorer.bytecode;

class OpenJdkToolsAssemblyTest extends AssemblyTest {

	@Override
	Disassembler getDisassembler() {
		return new OpenJdkJasmDisassembler();
	}

	@Override
	Assembler getAssembler() {
		return new OpenJdkJasmAssembler();
	}

}
