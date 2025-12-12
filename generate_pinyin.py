#!/usr/bin/env python3
"""
Generate pinyin pronunciations for the 2000 popular Chinese characters.
This script creates a Clojure-compatible EDN file with pinyin data.
"""

import re
import sys
from pathlib import Path

# Try to import pypinyin, install if not available
try:
    from pypinyin import pinyin, Style, lazy_pinyin
except ImportError:
    print("pypinyin not found. Installing...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "pypinyin"])
    from pypinyin import pinyin, Style, lazy_pinyin

def extract_chars_from_clojure():
    """Extract the 2000 characters from the Clojure source file."""
    dict_file = Path("src/main/cangjie_training/dictionary.cljs")
    if not dict_file.exists():
        print(f"Error: {dict_file} not found")
        return []
        
    content = dict_file.read_text(encoding='utf-8')
    
    # Find the popular-chinese-chars vector using a more robust approach
    # Look for the pattern: (def popular-chinese-chars ... [ ... ])
    lines = content.split('\n')
    in_vector = False
    chars_content = []
    
    for line in lines:
        if '(def popular-chinese-chars' in line:
            in_vector = True
            continue
        
        if in_vector:
            if line.strip().startswith('['):
                # Start collecting from this line
                chars_content.append(line)
            elif ']' in line and not line.strip().startswith(';'):
                # End of vector
                chars_content.append(line.split(']')[0])
                break
            elif in_vector:
                chars_content.append(line)
    
    if not chars_content:
        print("Could not find popular-chinese-chars vector in dictionary.cljs")
        return []
    
    # Combine all lines and extract quoted characters
    combined = '\n'.join(chars_content)
    chars = re.findall(r'"([^"]+)"', combined)
    
    # Clean up - remove any non-Chinese characters and duplicates
    chinese_chars = []
    seen = set()
    for char in chars:
        if len(char) == 1 and 0x4e00 <= ord(char) <= 0x9fff:  # Chinese character range
            if char not in seen:
                chinese_chars.append(char)
                seen.add(char)
    
    return chinese_chars

def get_pinyin_for_char(char):
    """Get pinyin for a single character with tone numbers."""
    try:
        # Get pinyin with tone numbers (yī, èr, sān, sì)
        result = pinyin(char, style=Style.TONE, heteronym=True)
        if result and result[0]:
            # Return all possible pronunciations for heteronyms
            return result[0]
        else:
            return ["?"]  # Unknown pinyin
    except Exception as e:
        print(f"Error getting pinyin for {char}: {e}")
        return ["?"]

def create_pinyin_mapping(chars):
    """Create a pinyin mapping for all characters."""
    pinyin_map = {}
    
    for i, char in enumerate(chars, 1):
        if i % 100 == 0:
            print(f"Processing character {i}/{len(chars)}: {char}")
            
        pinyin_list = get_pinyin_for_char(char)
        
        # For characters with multiple pronunciations, join them with "/"
        if len(pinyin_list) > 1:
            pinyin_str = "/".join(pinyin_list)
        else:
            pinyin_str = pinyin_list[0]
            
        pinyin_map[char] = pinyin_str
    
    return pinyin_map

def write_edn_file(pinyin_map, output_file="src/main/cangjie_training/pinyin.cljs"):
    """Write the pinyin data as a Clojure EDN file."""
    
    # Create the Clojure namespace and data structure
    edn_content = '''(ns cangjie-training.pinyin)

(def pinyin-dict
  "Pinyin pronunciations for popular Chinese characters from pypinyin"
  {
'''
    
    # Add each character-pinyin pair
    for char, pinyin in pinyin_map.items():
        # Escape any special characters in pinyin
        safe_pinyin = pinyin.replace('"', '\\"')
        edn_content += f'   "{char}" "{safe_pinyin}"\n'
    
    edn_content += '})\n'
    
    # Write to file
    output_path = Path(output_file)
    output_path.write_text(edn_content, encoding='utf-8')
    print(f"\n✅ Created pinyin dictionary with {len(pinyin_map)} entries at {output_file}")
    
    return output_path

def create_sample_file(pinyin_map, sample_size=20):
    """Create a small sample file for testing."""
    sample_chars = list(pinyin_map.items())[:sample_size]
    
    sample_content = '''(ns cangjie-training.pinyin-sample)

;; Sample pinyin data for testing
(def pinyin-sample
  {
'''
    
    for char, pinyin in sample_chars:
        sample_content += f'   "{char}" "{pinyin}"\n'
        
    sample_content += '})\n'
    
    sample_path = Path("src/main/cangjie_training/pinyin_sample.cljs")
    sample_path.write_text(sample_content, encoding='utf-8')
    print(f"✅ Created sample file with {sample_size} entries at {sample_path}")

def main():
    print("🔍 Extracting Chinese characters from dictionary.cljs...")
    chars = extract_chars_from_clojure()
    
    if not chars:
        print("❌ No characters found. Exiting.")
        return 1
    
    print(f"📊 Found {len(chars)} Chinese characters")
    print(f"🎯 First few characters: {' '.join(chars[:10])}")
    
    print("\n🔤 Generating pinyin mappings...")
    pinyin_map = create_pinyin_mapping(chars)
    
    print(f"\n📝 Sample pinyin mappings:")
    for char, pinyin in list(pinyin_map.items())[:5]:
        print(f"  {char} → {pinyin}")
    
    print("\n📁 Writing EDN files...")
    output_file = write_edn_file(pinyin_map)
    create_sample_file(pinyin_map)
    
    print(f"\n🎉 All done!")
    print(f"📋 Total characters processed: {len(pinyin_map)}")
    print(f"📁 Main output file: {output_file}")
    print(f"📄 Sample file: src/main/cangjie_training/pinyin_sample.cljs")
    
    print("\n💡 Next steps:")
    print("1. Review the generated pinyin data")
    print("2. Integrate pinyin.cljs into your ClojureScript app")
    print("3. Update the UI to display pinyin pronunciations")
    print("4. Consider adding tone color coding or audio pronunciation")
    
    return 0

if __name__ == "__main__":
    sys.exit(main())