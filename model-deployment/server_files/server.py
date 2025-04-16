from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import time
from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig, pipeline
import torch

app = FastAPI()


model_id = "FYPFAST/Llama-3.2-3B-Instruct-PEP8-Vulnerability-Python"

quant_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_compute_dtype=torch.bfloat16,
    bnb_4bit_use_double_quant=True,
    bnb_4bit_quant_type="nf4",
)

model = AutoModelForCausalLM.from_pretrained(
    model_id,
    device_map="auto",
    quantization_config=quant_config,
)

tokenizer = AutoTokenizer.from_pretrained(model_id)

generator = pipeline(
    "text-generation",
    model=model,
    tokenizer=tokenizer
)

class PromptRequest(BaseModel):
    prompt: str


@app.post("/generate")
async def generate_text(request: PromptRequest):
    if not request.prompt:
        raise HTTPException(status_code=400, detail="No prompt provided")
    print(request.prompt)
    start_time = time.time()

    output = generator([{"role": "user", "content": request.prompt}], max_new_tokens=1024, return_full_text=False)[0]

    print(output["generated_text"])
    end_time = time.time()
    response_time = end_time - start_time
    print(f"Response Time: {response_time:.2f} seconds")

    return output["generated_text"]

