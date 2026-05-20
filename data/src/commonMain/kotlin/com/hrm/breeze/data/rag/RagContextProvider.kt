package com.hrm.breeze.data.rag

class RagContextProvider(
    private val retriever: RagRetriever,
) {
    suspend fun retrieveContext(query: String): RetrievedContext =
        retriever.retrieve(query, RetrievalScope())
}
