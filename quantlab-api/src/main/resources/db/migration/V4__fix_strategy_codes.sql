-- Fix strategy codes to match the actual strategy CODE constants
-- This migration updates the strategy codes in the database to match the @Qualifier values

-- Fix EOD_BREAKOUT_VOL -> EOD_BREAKOUT
UPDATE strategy SET code = 'EOD_BREAKOUT' WHERE code = 'EOD_BREAKOUT_VOL';

-- Fix SMA_20_50_CROSS -> SMA_CROSSOVER
UPDATE strategy SET code = 'SMA_CROSSOVER' WHERE code = 'SMA_20_50_CROSS';

-- Fix NR4_INSIDE -> NR4_INSIDE_BAR
UPDATE strategy SET code = 'NR4_INSIDE_BAR' WHERE code = 'NR4_INSIDE';
