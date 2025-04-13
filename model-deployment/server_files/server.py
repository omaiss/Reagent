from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig
import torch
import os

app = FastAPI()

# Check if GPU is available
device = "cuda" if torch.cuda.is_available() else "cpu"
print(f"Using device: {device}")

# Model details
model_id = "FYPFAST/Llama-3.2-3B-Instruct-PEP8-Vulnerability-Python"
local_model_path = "./fine-tuned-model"

# Check if model is already downloaded
if not os.path.exists(local_model_path):
    print("Downloading model and tokenizer...")
    model = AutoModelForCausalLM.from_pretrained(model_id)
    tokenizer = AutoTokenizer.from_pretrained(model_id)

    print(f"Saving model and tokenizer to {local_model_path}...")
    model.save_pretrained(local_model_path)
    tokenizer.save_pretrained(local_model_path)
    print("Download and save completed!")
else:
    print("Loading model from local directory...")

# Quantization configuration
quantization_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_use_double_quant=True,
    bnb_4bit_quant_type="nf4",
    bnb_4bit_compute_dtype=torch.bfloat16
)

# Load the model and tokenizer
model = AutoModelForCausalLM.from_pretrained(
    local_model_path,
    quantization_config=quantization_config,
    device_map="auto",
)
tokenizer = AutoTokenizer.from_pretrained(local_model_path)


class CodeRequest(BaseModel):
    code: str


@app.post("/analyze")
def analyze_code(request: CodeRequest):
    try:
        # Prepare the input prompt
        prompt = f"Check the following code for PEP8 standard violations and any vulnerabilities. Return the corrected, PEP8-compliant, and vulnerability-free code:\n\n{request.code}"

        # Tokenize the input
        inputs = tokenizer(prompt, return_tensors="pt").to("cuda")

        # Generate the output
        outputs = model.generate(**inputs, max_new_tokens=500)
        response = tokenizer.decode(outputs[0], skip_special_tokens=True)

        return {"response": response}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
