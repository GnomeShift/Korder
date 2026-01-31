CREATE TABLE stock (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL UNIQUE REFERENCES products(id) ON DELETE CASCADE,
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT check_reserved_not_exceed_quantity CHECK (reserved_quantity <= quantity)
);

CREATE INDEX idx_stock_product_id ON stock(product_id);
CREATE INDEX idx_stock_available ON stock((quantity - reserved_quantity)) WHERE (quantity - reserved_quantity) > 0;
